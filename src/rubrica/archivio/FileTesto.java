package rubrica.archivio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Due cose che servono a tutti i file di testo del programma.
 */
final class FileTesto {

    private FileTesto() {
    }

    /**
     * Il Blocco note di Windows mette un BOM invisibile all'inizio del file.
     * Se resta, il primo nome comincia con un carattere che non si vede.
     */
    static String senzaBom(String riga) {
        if (riga != null && riga.length() > 0 && riga.charAt(0) == '\uFEFF') {
            return riga.substring(1);
        }
        return riga;
    }

    /**
     * Scrivo prima su un file .tmp accanto e poi lo rinomino sopra quello vero.
     * Se il programma si chiude a metà, il file vecchio resta intero.
     */
    static void scriviRighe(File file, List<String> righe) throws IOException {
        File tmp = preparaTemporaneo(file, righe);
        sostituisci(tmp, file);
    }

    /**
     * Prima metà di scriviRighe: il .tmp è pronto ma il file vero non è ancora toccato.
     */
    static File preparaTemporaneo(File file, List<String> righe) throws IOException {
        File tmp = new File(file.getAbsoluteFile().getParentFile(), file.getName() + ".tmp");
        PrintStream out = new PrintStream(new FileOutputStream(tmp), true, "UTF-8");
        try {
            for (int i = 0; i < righe.size(); i++) {
                out.println(righe.get(i));
            }
            if (out.checkError()) {
                throw new IOException("Scrittura fallita: " + tmp.getAbsolutePath());
            }
        } catch (IOException e) {
            out.close();
            tmp.delete();
            throw e;
        } finally {
            out.close();
        }
        return tmp;
    }

    static void sostituisci(File tmp, File file) throws IOException {
        try {
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
