package ruby.dev;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dev-only agent. Loaded into the Minecraft JVM so compiled classes can be
 * swapped in without restarting. Not packaged into the mod jar.
 */
public final class HotswapAgent {
	private static final Object LOCK = new Object();
	private static Instrumentation instrumentation;
	private static Path classesDir = Path.of("build/classes/java/main");
	private static final Map<Path, Integer> seenHash = new HashMap<>();

	public static void premain(String agentArgs, Instrumentation inst) {
		instrumentation = inst;
		int port = Integer.getInteger("ruby.hotswap.port", 5006);
		if (agentArgs != null && !agentArgs.isBlank()) {
			int eq = agentArgs.indexOf('=');
			if (eq > 0 && agentArgs.substring(0, eq).chars().allMatch(Character::isDigit)) {
				port = Integer.parseInt(agentArgs.substring(0, eq));
				classesDir = Path.of(agentArgs.substring(eq + 1));
			} else {
				classesDir = Path.of(agentArgs);
			}
		}
		classesDir = classesDir.toAbsolutePath().normalize();
		try {
			snapshot(classesDir);
		} catch (IOException e) {
			System.out.println("[ruby-hotswap] could not snapshot " + classesDir + ": " + e.getMessage());
		}
		int listenPort = port;
		Thread thread = new Thread(() -> listen(listenPort), "ruby-hotswap");
		thread.setDaemon(true);
		thread.start();
		System.out.println("[ruby-hotswap] listening on 127.0.0.1:" + listenPort + " classes=" + classesDir);
	}

	public static void main(String[] args) throws Exception {
		int port = Integer.getInteger("ruby.hotswap.port", 5006);
		boolean ping = args.length > 0 && "--ping".equals(args[0]);
		String dir = "";
		String stamp = "";
		if (!ping) {
			dir = args.length > 0 ? args[0] : "";
			stamp = args.length > 1 ? args[1] : "";
		}
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("127.0.0.1", port), 3000);
			Writer out = new OutputStreamWriter(socket.getOutputStream());
			out.write(ping ? "PING\n" : "HOTSWAP " + dir + "\n");
			out.flush();
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String line = in.readLine();
			if (line == null) {
				System.err.println("Dev client closed the hotswap connection.");
				System.exit(1);
			}
			System.out.println(line);
			if (line.startsWith("OK") && !stamp.isBlank()) {
				Path stampPath = Path.of(stamp);
				Files.createDirectories(stampPath.getParent());
				Files.writeString(stampPath, Long.toString(System.currentTimeMillis()));
			}
			if (!line.startsWith("OK") && !line.startsWith("PONG")) System.exit(1);
		} catch (IOException e) {
			System.err.println("No dev client is listening on 127.0.0.1:" + port + ". Start one with ./dev.sh client");
			System.exit(2);
		}
	}

	private static void listen(int port) {
		try (ServerSocket server = new ServerSocket()) {
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress(InetAddress.getLoopbackAddress(), port));
			while (true) {
				try (Socket socket = server.accept()) {
					socket.setSoTimeout(30000);
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					Writer out = new OutputStreamWriter(socket.getOutputStream());
					String line = in.readLine();
					String response = "ERROR empty request";
					if (line != null) {
						if ("PING".equals(line.trim())) response = "PONG";
						else if (line.startsWith("HOTSWAP")) {
							String extra = line.substring("HOTSWAP".length()).trim();
							Path dir = extra.isEmpty() ? classesDir : Path.of(extra);
							response = reload(dir.toAbsolutePath().normalize());
						} else {
							response = "ERROR unknown command";
						}
					}
					out.write(response);
					out.write("\n");
					out.flush();
				} catch (Exception e) {
					System.out.println("[ruby-hotswap] request failed: " + e);
				}
			}
		} catch (Exception e) {
			System.out.println("[ruby-hotswap] listener stopped: " + e);
		}
	}

	private static void snapshot(Path root) throws IOException {
		if (!Files.isDirectory(root)) return;
		try (var walk = Files.walk(root)) {
			for (Path path : walk.filter(HotswapAgent::isClassFile).toList()) {
				Path abs = path.toAbsolutePath().normalize();
				seenHash.put(abs, hash(Files.readAllBytes(abs)));
			}
		}
	}

	private static String reload(Path root) {
		synchronized (LOCK) {
			if (!Files.isDirectory(root)) return "ERROR classes directory missing: " + root;
			List<Path> changed = new ArrayList<>();
			Map<Path, byte[]> bytes = new HashMap<>();
			Map<Path, Integer> hashes = new HashMap<>();
			try (var walk = Files.walk(root)) {
				for (Path path : walk.filter(HotswapAgent::isClassFile).toList()) {
					Path abs = path.toAbsolutePath().normalize();
					byte[] data = Files.readAllBytes(abs);
					int hash = hash(data);
					if (hash == seenHash.getOrDefault(abs, Integer.MIN_VALUE)) continue;
					changed.add(abs);
					bytes.put(abs, data);
					hashes.put(abs, hash);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (changed.isEmpty()) return "OK redefined=0 pending=0 failed=0";

			Map<String, Class<?>> loaded = new HashMap<>();
			for (Class<?> type : instrumentation.getAllLoadedClasses()) {
				loaded.putIfAbsent(type.getName(), type);
			}

			int redefined = 0;
			int pending = 0;
			List<String> failed = new ArrayList<>();
			for (Path path : changed) {
				String name = className(root, path);
				int hash = hashes.get(path);
				Class<?> type = loaded.get(name);
				if (type == null) {
					pending++;
					seenHash.put(path, hash);
					continue;
				}
				if (!instrumentation.isModifiableClass(type)) {
					failed.add(name + ": not modifiable");
					continue;
				}
				try {
					instrumentation.redefineClasses(new ClassDefinition(type, bytes.get(path)));
					seenHash.put(path, hash);
					redefined++;
					System.out.println("[ruby-hotswap] redefined " + name);
				} catch (UnmodifiableClassException | ClassNotFoundException | UnsupportedOperationException | VerifyError | ClassFormatError e) {
					seenHash.put(path, hash);
					failed.add(name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
					System.out.println("[ruby-hotswap] failed " + name + ": " + e);
				} catch (Throwable e) {
					failed.add(name + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
					System.out.println("[ruby-hotswap] failed " + name + ": " + e);
				}
			}
			boolean onlyMixins = failed.stream().allMatch(line -> line.startsWith("ruby.mixin."));
			String status = failed.isEmpty() || onlyMixins ? "OK" : "FAIL";
			String message = status + " redefined=" + redefined + " pending=" + pending + " failed=" + failed.size();
			if (!failed.isEmpty()) {
				int limit = Math.min(failed.size(), 8);
				message += " failures=" + String.join(" | ", failed.subList(0, limit));
			}
			return message.replace('\n', ' ');
		}
	}

	private static boolean isClassFile(Path path) {
		return path.getFileName().toString().endsWith(".class");
	}

	private static int hash(byte[] data) {
		int hash = 1;
		for (byte value : data) hash = 31 * hash + value;
		return hash;
	}

	private static String className(Path root, Path file) {
		String rel = root.relativize(file).toString();
		return rel.substring(0, rel.length() - ".class".length()).replace('/', '.').replace('\\', '.');
	}
}
