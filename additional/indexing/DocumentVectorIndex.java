import java.io.File;
import java.io.IOException;
import java.util.*;

public class DocumentVectorIndex extends BinaryDataFile<TFIDFSearcher.DocumentVector, HashSet<TFIDFSearcher.DocumentVector>, List<DocumentVectorPointer>>{

    public DocumentVectorIndex(File scoreIndexFile){
        super(scoreIndexFile);
    }

    public void write(TFIDFSearcher.DocumentVector documentVector){
        try {
            randomAccessFile.writeInt(documentVector.getDocId());
            randomAccessFile.writeInt(documentVector.getVector().size());
            for (Map.Entry<Integer, Double> entry : documentVector.getVector().entrySet()){
                randomAccessFile.writeInt(entry.getKey());
                randomAccessFile.writeDouble(entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<DocumentVectorPointer> writeAll(HashSet<TFIDFSearcher.DocumentVector> documentVectors){
        try {
            if (randomAccessFile.length() != 0){
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        List<DocumentVectorPointer> pointers = new ArrayList<>();
        for (TFIDFSearcher.DocumentVector d : documentVectors) {
            try {
                pointers.add(new DocumentVectorPointer(d.getDocId(), randomAccessFile.getFilePointer(), d.getVector().size()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            write(d);
        }

        return pointers;
    }

    public TFIDFSearcher.DocumentVector read(){
        try {
            int documentId = randomAccessFile.readInt();
            int mapSize = randomAccessFile.readInt();

            HashMap<Integer, Double> hashMap = new HashMap<>();
            for (int i = 0; i < mapSize; i++){
                int termId = randomAccessFile.readInt();
                double score = randomAccessFile.readDouble();
                hashMap.put(termId, score);
            }

            return new TFIDFSearcher.DocumentVector(documentId, hashMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public HashSet<TFIDFSearcher.DocumentVector> readAll() throws IOException{
        HashSet<TFIDFSearcher.DocumentVector> documentVectors = new HashSet<>();
        while (randomAccessFile.getFilePointer() < randomAccessFile.length()){
            documentVectors.add(read());
        }
        return documentVectors;
    }
}
