import java.io.File;
import java.io.IOException;
import java.util.HashSet;

public class VectorDictionary extends BinaryDataFile<DocumentVectorPointer, HashSet<DocumentVectorPointer>, Boolean> {

    public VectorDictionary(File scoreIndexFile) {
        super(scoreIndexFile);
    }

    @Override
    void write(DocumentVectorPointer element) {
        try {
            randomAccessFile.writeInt(element.getDocId());
            randomAccessFile.writeLong(element.getBytePosition());
            randomAccessFile.writeInt(element.getDocFreq());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    Boolean writeAll(HashSet<DocumentVectorPointer> dataStructure) {
        for (DocumentVectorPointer documentVectorPointer: dataStructure){
            write(documentVectorPointer);
        }
        return true;
    }

    @Override
    DocumentVectorPointer read() {
        try {
            int docId = randomAccessFile.readInt();
            long bytePosition = randomAccessFile.readLong();
            int docFreq = randomAccessFile.readInt();

            return new DocumentVectorPointer(docId, bytePosition, docFreq);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    HashSet<DocumentVectorPointer> readAll() throws IOException {
        HashSet<DocumentVectorPointer> pointers = new HashSet<>();
        while (randomAccessFile.getFilePointer() < randomAccessFile.length()){
            pointers.add(read());
        }
        return pointers;
    }
}