import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class HuffmanTree {
    private HuffmanNode root;

    public HuffmanTree(byte[] data) {
        this.root = buildTree(data);
    }

    private HuffmanNode buildTree(byte[] data) {
        Map<Byte, Integer> frequencyMap = calculateFrequency(data);

        PriorityQueue<HuffmanNode> priorityQueue = new PriorityQueue<>();
        for (Map.Entry<Byte, Integer> entry : frequencyMap.entrySet()) {
            priorityQueue.add(new HuffmanNode(entry.getKey(), entry.getValue()));
        }

        while (priorityQueue.size() > 1) {
            HuffmanNode left = priorityQueue.poll();
            HuffmanNode right = priorityQueue.poll();
            HuffmanNode parent = new HuffmanNode((byte) 0, left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;
            priorityQueue.add(parent);
        }

        return priorityQueue.poll();
    }

    private Map<Byte, Integer> calculateFrequency(byte[] data) {
        Map<Byte, Integer> frequencyMap = new HashMap<>();
        for (byte b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
        }
        return frequencyMap;
    }

    public Map<Byte, String> generateHuffmanCodes() {
        Map<Byte, String> codes = new HashMap<>();
        generateCodesRecursive(root, "", codes);
        return codes;
    }

    private void generateCodesRecursive(HuffmanNode node, String code, Map<Byte, String> codes) {
        if (node != null) {
            if (node.left == null && node.right == null) {
                codes.put(node.data, code);
            }
            generateCodesRecursive(node.left, code + "0", codes);
            generateCodesRecursive(node.right, code + "1", codes);
        }
    }

    public HuffmanNode getRoot() {
        return root;
    }
}
