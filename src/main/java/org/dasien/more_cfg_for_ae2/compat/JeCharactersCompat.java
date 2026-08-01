package org.dasien.more_cfg_for_ae2.compat;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * 拼音搜索兼容（Just Enough Characters 提供 PinIn 库）。
 * <p>
 * PinIn 由 JEC 以 jarjar 形式提供（模块名 PinIn），本类不直接引用 PinIn 类型，
 * 改为反射调用，避免本模组重复打包 PinIn 导致 JPMS 模块重复导出冲突
 * （Modules pinin and PinIn export package ... to module pinyin4j）。
 * 未安装 JEC（或 PinIn 不可用）时自动降级为普通文本匹配。
 */
public final class JeCharactersCompat {
    private static boolean checked;
    private static Object pinIn;
    private static Method contains;
    private static Method begins;
    private static Method matches;

    private JeCharactersCompat() {
    }

    public static boolean matches(String text, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        if (text == null || text.isBlank()) {
            return false;
        }
        Object pinIn = pinIn();
        if (pinIn != null) {
            try {
                if ((Boolean) contains.invoke(pinIn, text, query)
                        || (Boolean) begins.invoke(pinIn, text, query)
                        || (Boolean) matches.invoke(pinIn, text, query)) {
                    return true;
                }
            } catch (ReflectiveOperationException ignored) {
                // 反射失败时降级为普通匹配
            }
        }
        return text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private static Object pinIn() {
        if (!checked) {
            checked = true;
            try {
                Class<?> pinInClass = Class.forName("me.towdium.pinin.PinIn");
                Object instance = pinInClass.getConstructor().newInstance();
                contains = pinInClass.getMethod("contains", String.class, String.class);
                begins = pinInClass.getMethod("begins", String.class, String.class);
                matches = pinInClass.getMethod("matches", String.class, String.class);
                pinIn = instance;
            } catch (ReflectiveOperationException ignored) {
                pinIn = null;
            }
        }
        return pinIn;
    }
}
