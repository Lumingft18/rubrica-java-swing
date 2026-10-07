-- Rubrica. Lanciare questo script su MySQL prima di passare a modalita=database.
-- Crea il database, le due tabelle e l'utente di prova admin / admin.
-- La password admin entra in chiaro: al primo login il programma la sostituisce con l'hash.

CREATE DATABASE IF NOT EXISTS rubrica
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE rubrica;

CREATE TABLE IF NOT EXISTS persona (
  id INT NOT NULL AUTO_INCREMENT,
  nome VARCHAR(80) NOT NULL,
  cognome VARCHAR(80) NOT NULL,
  indirizzo VARCHAR(200) NOT NULL,
  telefono VARCHAR(40) NOT NULL,
  eta INT NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS utente (
  id INT NOT NULL AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB;

INSERT INTO utente (username, password)
SELECT 'admin', 'admin'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM utente WHERE username = 'admin');
