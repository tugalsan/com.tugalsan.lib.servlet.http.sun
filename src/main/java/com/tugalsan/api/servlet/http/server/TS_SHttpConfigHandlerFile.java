package com.tugalsan.api.servlet.http.server;

import com.tugalsan.api.coronator.client.TGS_Coronator;
import com.tugalsan.api.string.client.TGS_StringUtils;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;
import java.nio.file.Path;

public class TS_SHttpConfigHandlerFile {

    public TS_SHttpConfigHandlerFile(String slash_path_slash, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, Path root) {
        this.slash_path_slash = TGS_Coronator.ofStr()
                .anoint(val -> slash_path_slash)
                .anointIf(TGS_StringUtils::isNullOrEmpty, val -> "/")
                .anointIf(val -> val.indexOf(0) != '/', val -> "/" + val)
                .anointIf(val -> val.indexOf(val.length() - 1) != '/', val -> val + "/")
                .coronate();
        this.allow = allow;
        this.root = root;
    }
    final public String slash_path_slash;
    final public TGS_ValidatorType1<TS_SHttpHandlerRequest> allow;
    final public Path root;

    public static TS_SHttpConfigHandlerFile of(String slash_path_slash, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, Path root) {
        return new TS_SHttpConfigHandlerFile(slash_path_slash, allow, root);
    }
}
