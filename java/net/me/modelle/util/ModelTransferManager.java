package net.me.modelle.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelTransferManager {
    private static class TransferState {
        final byte[] data;
        int received = 0;

        TransferState(int totalSize) {
            this.data = new byte[totalSize];
        }
    }

    private static final Map<String, TransferState> transfers = new ConcurrentHashMap<>();

    public static void startReception(String hash, int totalSize) {
        // 🔒 Отказываем в приёме некорректных или слишком больших файлов
        if (totalSize <= 0 || totalSize > ModelManager.MAX_MODEL_SIZE) {
            System.err.println("[Modelle] Отказ в приёме файла: недопустимый размер " + totalSize);
            return;
        }
        transfers.put(hash, new TransferState(totalSize));
    }

    public static boolean receiveChunk(String hash, int offset, byte[] data) {
        if (data == null || data.length == 0) return false;
        TransferState state = transfers.get(hash);
        if (state == null) return false;

        // 🔒 Проверка границ
        if (offset < 0 || offset >= state.data.length || offset + data.length > state.data.length) {
            System.err.println("[Modelle] Chunk вне границ файла: hash=" + hash + " offset=" + offset);
            transfers.remove(hash); // Удаляем битую передачу
            return false;
        }

        System.arraycopy(data, 0, state.data, offset, data.length);
        state.received += data.length;

        return state.received >= state.data.length;
    }

    public static byte[] getCompleteData(String hash) {
        TransferState state = transfers.remove(hash);
        return state != null ? state.data : null;
    }
}