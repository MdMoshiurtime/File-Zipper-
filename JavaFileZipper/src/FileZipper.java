import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

class FileIO {

    public static byte[] readFile(String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            return bos.toByteArray();
        }
    }

    public static void writeToFile(String filePath, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(data);
        }
    }
}

class HuffmanNode implements Comparable<HuffmanNode> {
    byte data;
    int frequency;
    HuffmanNode left;
    HuffmanNode right;

    public HuffmanNode(byte data, int frequency) {
        this.data = data;
        this.frequency = frequency;
    }

    @Override
    public int compareTo(HuffmanNode other) {
        return this.frequency - other.frequency;
    }
}

class HuffmanTree {
    private HuffmanNode root;

    public HuffmanTree(byte[] data) {
        buildTree(data);
    }

    private void buildTree(byte[] data) {
        // Count frequency of each byte in the data
        int[] frequency = new int[256];
        for (byte b : data) {
            frequency[b & 0xFF]++;
        }

        // Create Huffman nodes for each non-zero frequency byte
        PriorityQueue<HuffmanNode> priorityQueue = new PriorityQueue<>();
        for (int i = 0; i < 256; i++) {
            if (frequency[i] > 0) {
                priorityQueue.add(new HuffmanNode((byte) i, frequency[i]));
            }
        }

        // Build the Huffman tree
        while (priorityQueue.size() > 1) {
            HuffmanNode left = priorityQueue.poll();
            HuffmanNode right = priorityQueue.poll();

            HuffmanNode parent = new HuffmanNode((byte) 0, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;

            priorityQueue.add(parent);
        }

        // The remaining node is the root of the Huffman tree
        root = priorityQueue.poll();
    }

    // Getter for the root of the Huffman tree
    public HuffmanNode getRoot() {
        return root;
    }
}

public class FileZipper {

    private static Map<Byte, String> encodingTable = new HashMap<>();
    private static HuffmanTree huffmanTree;

    public static void compress(String[] inputPaths, String[] zipFiles) {
        try {
            List<byte[]> dataList = new ArrayList<>();

            // Iterate through inputPaths and add data to the list
            for (String inputPath : inputPaths) {
                File inputFile = new File(inputPath);

                if (inputFile.isFile()) {
                    // If it's a file, add its data to the list
                    byte[] data = FileIO.readFile(inputPath);
                    dataList.add(data);
                } else if (inputFile.isDirectory()) {
                    // If it's a directory, add data from all files in the directory to the list
                    File[] files = inputFile.listFiles();

                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                byte[] data = FileIO.readFile(file.getAbsolutePath());
                                dataList.add(data);
                            }
                        }
                    }
                }
            }

            // Concatenate data from multiple files into a single byte array
            byte[] combinedData = concatenateData(dataList);

            // Build Huffman tree and encoding table
            huffmanTree = new HuffmanTree(combinedData);
            buildEncodingTable(huffmanTree.getRoot(), "");

            // Compress the combined data
            byte[] compressedData = compressData(combinedData);

            // Write the compressed data and encoding table to the zip file
            writeZipFile(zipFiles[0], compressedData);

            // Save the encoding table to a separate file (optional)
            saveEncodingTable("encodingTable.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

public static void decompress(String[] zipFiles, String[] outputFiles) {
    try {
        // Initialize HuffmanTree
        huffmanTree = new HuffmanTree(new byte[0]);

        // Read the compressed data and encoding table from the zip file
        byte[] compressedData = readZipFile(zipFiles[0]);  // Assuming only one zip file for decompression
        readEncodingTable("encodingTable.txt");

        // Decompress the data
        byte[] decompressedData = decompressData(compressedData);

        // Write decompressed data to the specified output file
        FileIO.writeToFile(outputFiles[0], decompressedData);

    } catch (IOException e) {
        e.printStackTrace();
    }
}
    private static byte[] concatenateData(List<byte[]> dataList) {
        int totalLength = dataList.stream().mapToInt(bytes -> bytes.length).sum();
        byte[] combinedData = new byte[totalLength];
        int destPos = 0;
        for (byte[] data : dataList) {
            System.arraycopy(data, 0, combinedData, destPos, data.length);
            destPos += data.length;
        }
        return combinedData;
    }

    private static void buildEncodingTable(HuffmanNode root, String code) {
        if (root != null) {
            if (root.left == null && root.right == null) {
                encodingTable.put(root.data, code);
            }
            buildEncodingTable(root.left, code + "0");
            buildEncodingTable(root.right, code + "1");
        }
    }

    private static byte[] compressData(byte[] data) {
        StringBuilder compressedBits = new StringBuilder();
        for (byte b : data) {
            compressedBits.append(encodingTable.get(b));
        }

        // Convert the binary string to a byte array
        int len = compressedBits.length();
        byte[] compressedBytes = new byte[(len + 7) / 8];

        for (int i = 0; i < len; i++) {
            if (compressedBits.charAt(i) == '1') {
                compressedBytes[i / 8] |= (1 << (7 - i % 8));
            }
        }

        return compressedBytes;
    }

private static byte[] decompressData(byte[] compressedData) {
    List<Byte> decompressedBytes = new ArrayList<>();
    HuffmanNode current = huffmanTree.getRoot();

    for (byte compressedByte : compressedData) {
        for (int i = 7; i >= 0; i--) {
            int bit = (compressedByte >> i) & 1;

            // Add null check before accessing the right or left field
            if (bit == 0) {
                if (current != null && current.left != null) {
                    current = current.left;
                }
            } else {
                if (current != null && current.right != null) {
                    current = current.right;
                }
            }

            if (current != null && current.left == null && current.right == null) {
                decompressedBytes.add(current.data);
                current = huffmanTree.getRoot();
            }
        }
    }

    // Convert the list of decompressed bytes to a byte array
    byte[] result = new byte[decompressedBytes.size()];
    for (int i = 0; i < decompressedBytes.size(); i++) {
        result[i] = decompressedBytes.get(i);
    }

    return result;
}

    private static void writeZipFile(String zipFile, byte[] compressedData) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zipOut = new ZipOutputStream(fos)) {

            ZipEntry compressedEntry = new ZipEntry("compressed.bin");
            zipOut.putNextEntry(compressedEntry);
            zipOut.write(compressedData);
            zipOut.closeEntry();
        }
    }

    private static byte[] readZipFile(String zipFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(zipFile);
             ZipInputStream zipIn = new ZipInputStream(fis)) {

            ZipEntry entry = zipIn.getNextEntry();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];

            while (entry != null) {
                int bytesRead = zipIn.read(buffer);
                while (bytesRead != -1) {
                    bos.write(buffer, 0, bytesRead);
                    bytesRead = zipIn.read(buffer);
                }
                entry = zipIn.getNextEntry();
            }

            return bos.toByteArray();
        }
    }

    private static void saveEncodingTable(String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            for (Map.Entry<Byte, String> entry : encodingTable.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        }
    }

    private static void readEncodingTable(String filePath) throws IOException {
        encodingTable.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length == 2) {
                    byte key = Byte.parseByte(parts[0]);
                    String value = parts[1];
                    encodingTable.put(key, value);
                }
            }
        }
    }

    public static void main(String[] args) {
        // Test your compression and decompression here

        // // Compressing a single file
        // String[] singleFileInput = {"C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\input.txt"};
        // String singleFileZip = "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\Compressed\\compressedSingleFile.zip";
        // compress(singleFileInput, new String[]{singleFileZip});

        // // Compressing multiple files
        // String[] multipleFilesInput = {"C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\input.txt", "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\me.c", "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\me.java", "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\me.py"};
        // String multipleFilesZip = "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\Compressed\\compressedMultipleFiles.zip";
        // compress(multipleFilesInput, new String[]{multipleFilesZip});

        // // Compressing a folder
        // String folderInput = "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test";
        // String folderZip = "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\Compressed\\compressedFolder.zip";
        // compress(new String[]{folderInput}, new String[]{folderZip});

        // Decompressing to a single output file
        String decompressZip = "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\Compressed\\compressedSingleFile.zip";
        String outputFile = "C:\\My WorkSpace\\Study\\JavaFilezipper\\JavaFileZipper\\src\\Test\\Decompressed\\outputFile.txt";
        decompress(new String[]{decompressZip}, new String[]{outputFile});


        // // Decompressing multiple folders
        // String[] decompressZips = {"compressedSingleFile.zip", "compressedMultipleFiles.zip"};
        // String[] outputFolders = {"outputFolder1", "outputFolder2"};
        // decompress(decompressZips, outputFolders);
    }
}
