package rubrica;

/**
 * Chi entra nel programma. Username e password, nient'altro.
 * password è la forma salvata: di solito l'hash di Password, in chiaro solo se scritta a mano.
 */
public class Utente {

    private final String username;
    private final String password;

    public Utente(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean coincide(String username, String password) {
        return this.username.equals(username) && Password.verifica(password, this.password);
    }
}
