package run;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

///
/// Quản lý vào/ra của tệp
///- Mở input: ưu tiên working directory; nếu không có thì đọc từ "cùng package" (classpath).
///- Xác định nơi ghi output: ưu tiên "cùng package"
///  (nếu resolve được đường dẫn file://),
///  nếu không thì fallback ghi ra working directory.
public class LogIO {
    private final String inputName;
    private final String outputName;

    //Lưu thư mục “cùng package”
    private Path packageDirCache;

    public LogIO(String inputName, String outputName) {
        this.inputName = inputName;
        this.outputName = outputName;
    }

    /// Mở Stream đọc file input
    /// - Nếu tồn tại ở working dir → đọc trực tiếp & lưu packageDirCache = parent
    /// - Nếu không → tìm trong classpath (cùng package hoặc root)
    public BufferedReader openInputReader() throws IOException {

        // Working directory
        Path p = Path.of(inputName);
        if (Files.exists(p)) {
            if (packageDirCache == null) packageDirCache = p.toAbsolutePath().getParent();
            return Files.newBufferedReader(p, StandardCharsets.UTF_8);
        }

        // Cùng package
        InputStream is = getClass().getResourceAsStream(inputName);
        if (is == null) {
            is = ClassLoader.getSystemResourceAsStream(inputName);
        }
        if (is == null) {
            throw new FileNotFoundException("Khong tim thay " + inputName + " o working dir hoac classpath.");
        }

        // Nếu resource là file:// -> xác định được package dir
        Path pkg = tryResolvePackageDirFromResource(inputName);
        if (pkg != null && packageDirCache == null) {
            packageDirCache = pkg;
        }

        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    /// Xác định nơi ghi output
    /// - Nếu đã biết packageDirCache → ghi ở đó
    /// - Nếu chưa, thử resolve lại từ resource
    /// - Sau cùng → working directory
    public Path resolveOutputPath() {
        if (packageDirCache != null) {
            try {
                Files.createDirectories(packageDirCache);
                return packageDirCache.resolve(outputName);
            } catch (Exception ignored) { }
        }
        // Thử detect lại từ resource (nếu input ở classpath file://)
        Path pkg = tryResolvePackageDirFromResource(inputName);
        if (pkg != null) {
            try {
                Files.createDirectories(pkg);
                packageDirCache = pkg;
                return pkg.resolve(outputName);
            } catch (Exception ignored) { }
        }
        // Cuối cùng: working dir
        return Path.of(outputName).toAbsolutePath();
    }

    /// Thử lấy URL resource tương đối (cùng package với run.LogIO.class), nếu là protocol file:// thì
    /// trả về parent (chính là thư mục “cùng package” trong filesystem).
    private Path tryResolvePackageDirFromResource(String resName) {
        try {
            URL url = getClass().getResource(resName);
            if (url == null) return null;
            if (!"file".equalsIgnoreCase(url.getProtocol())) return null;
            Path inputPath = Paths.get(url.toURI());
            return inputPath.getParent();
        } catch (URISyntaxException e) {
            return null;
        }
    }
}

