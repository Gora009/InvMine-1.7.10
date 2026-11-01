package com.invmine.client;

import cpw.mods.fml.common.Loader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

/**
 * Определяет, вводит ли игрок сейчас текст (поиск в креативе, имя в наковальне,
 * поиск NEI/AE2 и т.д.), чтобы WASD печатались, а не двигали игрока.
 * Работает рефлексией и ничего не меняет в чужих классах.
 */
final class TextInputDetector {

    private static final Map<Class<?>, Field[]> FIELDS = new HashMap<Class<?>, Field[]>();
    private static final Map<Class<?>, Method> FOCUS_METHODS = new HashMap<Class<?>, Method>();
    private static final Method NO_METHOD;
    private static final String[] FOCUS_METHOD_NAMES = {"isFocused", "focused", "getFocused", "isFocus", "hasFocus"};
    private static final String[] TEXT_CLASS_HINTS = {"textfield", "textbox", "searchfield", "searchbox", "textinput"};

    private static Field[] neiFields;
    private static boolean neiChecked;

    static {
        try {
            NO_METHOD = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private TextInputDetector() {}

    static boolean isTyping(GuiScreen screen) {
        return scanFields(fieldsOf(screen.getClass()), screen) || isNeiTyping();
    }

    private static boolean scanFields(Field[] fields, Object owner) {
        for (Field f : fields) {
            try {
                if (isFocusedValue(f.get(Modifier.isStatic(f.getModifiers()) ? null : owner))) {
                    return true;
                }
            } catch (Throwable ignored) {
                // недоступное поле — пропускаем
            }
        }
        return false;
    }

    private static boolean isFocusedValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Collection) {
            try {
                for (Object o : (Collection<?>) value) {
                    if (isFocusedInput(o)) {
                        return true;
                    }
                }
            } catch (Throwable ignored) {
                // ConcurrentModification и т.п. — пропускаем
            }
            return false;
        }
        if (value.getClass().isArray()) {
            if (value.getClass().getComponentType().isPrimitive()) {
                return false;
            }
            int len = Array.getLength(value);
            for (int i = 0; i < len; i++) {
                if (isFocusedInput(Array.get(value, i))) {
                    return true;
                }
            }
            return false;
        }
        return isFocusedInput(value);
    }

    private static boolean isFocusedInput(Object o) {
        if (o == null) {
            return false;
        }
        if (o instanceof GuiTextField) {
            GuiTextField tf = (GuiTextField) o;
            return tf.isFocused() && tf.getVisible();
        }
        Method m = focusMethod(o.getClass());
        if (m == NO_METHOD) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(m.invoke(o));
        } catch (Throwable t) {
            return false;
        }
    }

    /** Поля, которые могут содержать текстовые поля: сами поля ввода, их массивы и коллекции. */
    private static Field[] fieldsOf(Class<?> cls) {
        Field[] cached = FIELDS.get(cls);
        if (cached != null) {
            return cached;
        }
        List<Field> result = new ArrayList<Field>();
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            Field[] declared;
            try {
                declared = c.getDeclaredFields();
            } catch (Throwable t) {
                continue;
            }
            for (Field f : declared) {
                Class<?> t = f.getType();
                if (t.isPrimitive() || t == String.class) {
                    continue;
                }
                if (isCandidateType(t) || Collection.class.isAssignableFrom(t)
                    || (t.isArray() && isCandidateType(t.getComponentType()))) {
                    try {
                        f.setAccessible(true);
                        result.add(f);
                    } catch (Throwable ignored) {
                        // нет доступа — пропускаем
                    }
                }
            }
        }
        cached = result.toArray(new Field[0]);
        FIELDS.put(cls, cached);
        return cached;
    }

    private static boolean isCandidateType(Class<?> t) {
        return GuiTextField.class.isAssignableFrom(t) || looksLikeTextInput(t);
    }

    private static boolean looksLikeTextInput(Class<?> t) {
        for (Class<?> c = t; c != null && c != Object.class; c = c.getSuperclass()) {
            String n = c.getSimpleName().toLowerCase();
            for (String hint : TEXT_CLASS_HINTS) {
                if (n.contains(hint)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Ищет у поля ввода стороннего мода метод вида boolean isFocused()/focused(). */
    private static Method focusMethod(Class<?> cls) {
        Method m = FOCUS_METHODS.get(cls);
        if (m != null) {
            return m;
        }
        m = NO_METHOD;
        if (looksLikeTextInput(cls)) {
            outer:
            for (String name : FOCUS_METHOD_NAMES) {
                for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                    try {
                        Method candidate = c.getDeclaredMethod(name);
                        if (candidate.getReturnType() == boolean.class) {
                            candidate.setAccessible(true);
                            m = candidate;
                            break outer;
                        }
                    } catch (Throwable ignored) {
                        // ищем дальше
                    }
                }
            }
        }
        FOCUS_METHODS.put(cls, m);
        return m;
    }

    /** NEI держит поле поиска и поле количества статически в LayoutManager. */
    private static boolean isNeiTyping() {
        if (!neiChecked) {
            neiChecked = true;
            if (Loader.isModLoaded("NotEnoughItems")) {
                try {
                    List<Field> statics = new ArrayList<Field>();
                    for (Field f : fieldsOf(Class.forName("codechicken.nei.LayoutManager"))) {
                        if (Modifier.isStatic(f.getModifiers())) {
                            statics.add(f);
                        }
                    }
                    neiFields = statics.toArray(new Field[0]);
                } catch (Throwable ignored) {
                    neiFields = null;
                }
            }
        }
        return neiFields != null && scanFields(neiFields, null);
    }
}
