package com.tugalsan.api.servlet.http.server;

import com.tugalsan.api.servlet.http.server.TS_SHttpHandlerRequest;
import com.tugalsan.api.validator.client.TGS_ValidatorType1;
import java.nio.file.Path;

public class TS_SHttpConfigHandlerFile {

    public TS_SHttpConfigHandlerFile(String slash_path_slash, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, Path root) {
        this.slash_path_slash = slash_path_slash;
        this.allow = allow;
        this.root = root;
    }
    final public String slash_path_slash;
    final public TGS_ValidatorType1<TS_SHttpHandlerRequest> allow;
    final public Path root;

    public static TS_SHttpConfigHandlerFile of(String slash_path_slash, TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, Path root) {
        return new TS_SHttpConfigHandlerFile(slash_path_slash, allow, root);
    }

    public static TS_SHttpConfigHandlerFile of(TGS_ValidatorType1<TS_SHttpHandlerRequest> allow, Path root) {
        return of("/file/", allow, root);
    }
}
