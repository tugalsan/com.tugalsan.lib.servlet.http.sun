package com.tugalsan.api.servlet.http.sun.server;

import com.tugalsan.api.function.client.maythrow.uncheckedexceptions.TGS_FuncMTUCEEffectivelyFinal;
import com.tugalsan.api.function.client.maythrow.uncheckedexceptions.TGS_FuncMTUCE_OutBool_In1;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.string.client.TGS_StringUtils;
import java.nio.file.Path;

public class TS_SHttpConfigHandlerFile {

    final private static TS_Log d = TS_Log.of(false, TS_SHttpConfigHandlerFile.class);

    private TS_SHttpConfigHandlerFile(String slash_path_slash, TGS_FuncMTUCE_OutBool_In1<TS_SHttpHandlerRequest> allow, Path root, boolean filterUrlsWithHiddenChars) {
        this.slash_path_slash = TGS_FuncMTUCEEffectivelyFinal.ofStr()
                .anoint(val -> slash_path_slash)
                .anointIf(TGS_StringUtils.cmn()::isNullOrEmpty, val -> {
                    d.ci("constructor", "TGS_StringUtils::isNullOrEmpty", "set as '/'");
                    return "/";
                })
                .anointIf(val -> val.charAt(0) != '/', val -> {
                    d.ci("constructor", "val.charAt(0) != '/'", "add '/' to the left");
                    return "/" + val;
                })
                .anointIf(val -> val.charAt(val.length() - 1) != '/', val -> {
                    d.ci("constructor", "val.charAt(val.length() - 1) != '/'", "add '/' to the right");
                    return val + "/";
                })
                .coronate();
        d.ci("constructor", "slash_path_slash", slash_path_slash);
        this.allow = allow;
        this.root = root;
        this.filterUrlsWithHiddenChars = filterUrlsWithHiddenChars;
    }
    final public String slash_path_slash;
    final public TGS_FuncMTUCE_OutBool_In1<TS_SHttpHandlerRequest> allow;
    final public Path root;
    final public boolean filterUrlsWithHiddenChars;

    public static TS_SHttpConfigHandlerFile of(String slash_path_slash, TGS_FuncMTUCE_OutBool_In1<TS_SHttpHandlerRequest> allow, Path root, boolean filterUrlsWithHiddenChars) {
        return new TS_SHttpConfigHandlerFile(slash_path_slash, allow, root, filterUrlsWithHiddenChars);
    }

    public static TS_SHttpConfigHandlerFile ofEmpty() {
        return new TS_SHttpConfigHandlerFile(null, null, null, false);
    }

    public boolean isEmpty() {
        return root == null;
    }
}
