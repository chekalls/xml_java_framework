package mg.miniframework.web.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import mg.miniframework.logging.LogManager;
import mg.miniframework.logging.LogManager.LogStatus;

public class MultipartRequestReader {

    private final LogManager logManager;
    private final String fileSavePath;

    public MultipartRequestReader(LogManager logManager, String fileSavePath) {
        this.logManager = logManager;
        this.fileSavePath = fileSavePath == null ? "/public" : fileSavePath;
    }

    public MultipartRequestData read(HttpServletRequest request) throws Exception {
        MultipartRequestData data = new MultipartRequestData();

        Charset encoding = getRequestEncoding(request);
        for (Part part : request.getParts()) {
            String submittedName = part.getSubmittedFileName();
            boolean isFilePart = submittedName != null && !submittedName.isBlank();

            if (!isFilePart) {
                data.getFormParameters().put(part.getName(), readPartValue(part, encoding));
                continue;
            }

            String uploadPath = fileSavePath;
            String realUploadPath = request.getServletContext().getRealPath(uploadPath);
            Path uploadDir = Path.of(realUploadPath);
            Files.createDirectories(uploadDir);
            Path relativeUploadDir = Path.of(uploadPath);

            Path fileName = relativeUploadDir.resolve(submittedName);
            Path absoluteFileName = uploadDir.resolve(submittedName);
            if (fileName == null) {
                continue;
            }

            logManager.insertLog("fileName rested " + fileName.toAbsolutePath(), LogStatus.DEBUG);

            byte[] content = readPartBytes(part);
            data.getFileBytes().put(fileName, content);

            mg.miniframework.modules.File uploadFile = new mg.miniframework.modules.File();
            uploadFile.setAbsolutePath(absoluteFileName);
            uploadFile.setContextPath(fileName);
            uploadFile.setContent(content);
            uploadFile.setLogManager(logManager);

            data.getFileObjects().put(fileName, uploadFile);

            logManager.insertLog(
                    "uploaded file captured : " + fileName + " (" + content.length + " bytes)",
                    LogStatus.DEBUG);
        }

        return data;
    }

    private Charset getRequestEncoding(HttpServletRequest request) {
        String encoding = request.getCharacterEncoding();
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }

        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            return StandardCharsets.UTF_8;
        }
    }

    private String readPartValue(Part part, Charset encoding) throws IOException {
        try (InputStream input = part.getInputStream()) {
            byte[] bytes = input.readAllBytes();
            return new String(bytes, encoding);
        }
    }

    private byte[] readPartBytes(Part part) throws IOException {
        try (InputStream input = part.getInputStream()) {
            return input.readAllBytes();
        }
    }
}
