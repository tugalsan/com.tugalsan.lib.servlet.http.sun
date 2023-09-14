package com.tugalsan.api.servlet.http.server;

import com.tugalsan.api.coronator.client.TGS_Coronator;
import com.tugalsan.api.log.server.TS_Log;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;
import java.nio.file.Path;

public class TS_SHttpConfigHandlerFile {

    final private static TS_Log d = TS_Log.of(true, TS_SHttpConfigHandlerFile.class);

    public TS_SHttpConfigHandlerFile(String slash_path_slash, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, Path root, boolean filterUrlsWithHiddenChars) {
        this.slash_path_slash = TGS_Coronator.ofStr()
                .anoint(val -> slash_path_slash)
                .anointIf(TGS_StringUtils::isNullOrEmpty, val -> {
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
    final public TGS_ValidatorType1<TS_SHttpHandlerRequest> allow;
    final public Path root;
    final public boolean filterUrlsWithHiddenChars;

    public static TS_SHttpConfigHandlerFile of(String slash_path_slash, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, Path root, boolean filterUrlsWithHiddenChars) {
        return new TS_SHttpConfigHandlerFile(slash_path_slash, allow, root, filterUrlsWithHiddenChars);
    }
}
