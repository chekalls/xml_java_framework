package mg.miniframework.web.multipart;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MultipartRequestData {

    private final Map<String, Object> formParameters = new HashMap<>();
    private final Map<Path, byte[]> fileBytes = new HashMap<>();
    private final Map<Path, mg.miniframework.modules.File> fileObjects = new HashMap<>();

    public Map<String, Object> getFormParameters() {
        return formParameters;
    }

    public Map<Path, byte[]> getFileBytes() {
        return fileBytes;
    }

    public Map<Path, mg.miniframework.modules.File> getFileObjects() {
        return fileObjects;
    }

}
