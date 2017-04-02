package server;

import com.db4o.Db4oEmbedded;
import com.db4o.ObjectContainer;
import com.db4o.ObjectSet;
import com.db4o.query.Predicate;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server Socket DB4o
 *
 * @author Manuel Martinez
 *         Copyright 2017, ManuMtz
 */

public class Server {
    private static final int PORT = 9090;
    private boolean end = false;

    private BufferedReader in = null;
    private PrintStream out = null;

    private static final String SP = "$##$";

    private static final String LS = System.lineSeparator();
    private static final int LS_SIZE = LS.length();

    private static final String FS = File.separator;

    private static final String ERROR = "ERROR";

    private static final String TANCARCONNEXIO = "TANCARCONNEXIO";
    private static final String RETORNCTRL = "RETORNCTRL";

    private static final String INSERT = "INSERT";
    private static final String DELETE = "DELETE";
    private static final String UPDATE = "UPDATE";
    private static final String SELECT = "SELECT";
    private static final String SELECTALL = "SELECTALL";

    private static final String RESULT = "RESULT";

    private static final String IDMSG = "IDMSG";
    private static final String MSG = "MSG";
    private static final String MSGFI = "MSGFI";

    private boolean farewellMessage = false;

    private static final String DBDIR = "database";
    private static final String DB = DBDIR + FS + "missatges.dbo";

    private static ObjectContainer db;


    private void listen() {
        ServerSocket serverSocket;
        Socket clientSocket;

        try {
            //Es crea un ServerSocket que atendrà el port nº PORT a l'espera de
            //clients que demanin comunicar-se.
            serverSocket = new ServerSocket(PORT);

            while (!end) {
                //El mètode accept resta a l'espera d'una petició i en el moment de
                //produir-se crea una instància específica de sòcol per suportar
                //la comunicació amb el client acceptat.
                clientSocket = serverSocket.accept();

                //Processem la petició del client.
                proccesClientRequest(clientSocket);

                //Tanquem el sòcol temporal per atendre el client.
                closeClient(clientSocket);
            }

            //Tanquem el sòcol principal
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void proccesClientRequest(Socket clientSocket) {
        String clientMessage = "";

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintStream(clientSocket.getOutputStream());

            do {
                farewellMessage = processData(clientMessage);

                if (farewellMessage) {
                    break;
                }

                StringBuilder missatge = new StringBuilder();
                String trosMissatgeTmp;

                while (((trosMissatgeTmp = in.readLine()) != null)) {
                    missatge.append(trosMissatgeTmp).append(LS);
                    if (trosMissatgeTmp.contains(MSGFI)) {
                        break;
                    }
                }

                for (int i = 0; i < LS_SIZE; i++) {
                    missatge.deleteCharAt(missatge.length() - 1);
                }

                clientMessage = missatge.toString();

            } while (!farewellMessage);

        } catch (IOException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    private boolean processData(String clientMessage) {
        try {
            if (!clientMessage.isEmpty()) {

                int idMsg = 0;
                String msg = "";

                boolean result;

                StringTokenizer st = new StringTokenizer(clientMessage, SP);
                String tipusMissatge = st.nextToken(SP);

                switch (tipusMissatge) {
                    case INSERT:

                        while (st.hasMoreTokens()) {
                            String tmp = st.nextToken();
                            if (tmp.equals(IDMSG)) {
                                idMsg = Integer.parseInt(st.nextToken());
                            } else if (tmp.equals(MSG)) {
                                msg = st.nextToken();
                            }
                        }

                        result = dbInsert(idMsg, msg);

                        if (result) {
                            out.println(RESULT + SP + MSG + SP + "INSERTAT" + SP + MSGFI);
                            out.flush();
                        } else {
                            out.println(RESULT + SP + MSG + SP + "JA EXISTEIX" + SP + MSGFI);
                            out.flush();
                        }

                        break;
                    case UPDATE:

                        while (st.hasMoreTokens()) {
                            String tmp = st.nextToken();
                            if (tmp.equals(IDMSG)) {
                                idMsg = Integer.parseInt(st.nextToken());
                            } else if (tmp.equals(MSG)) {
                                msg = st.nextToken();
                            }
                        }

                        result = dbUpdate(idMsg, msg);

                        if (result) {
                            out.println(RESULT + SP + MSG + SP + "ACTUALITZAT" + SP + MSGFI);
                            out.flush();
                        } else {
                            out.println(RESULT + SP + MSG + SP + "NO EXISTEIX" + SP + MSGFI);
                            out.flush();
                        }

                        break;
                    case DELETE:

                        while (st.hasMoreTokens()) {
                            String tmp = st.nextToken();
                            if (tmp.equals(IDMSG)) {
                                idMsg = Integer.parseInt(st.nextToken());
                            }
                        }

                        result = dbDelete(idMsg);

                        if (result) {
                            out.println(RESULT + SP + MSG + SP + "BORRAT" + SP + MSGFI);
                            out.flush();
                        } else {
                            out.println(RESULT + SP + MSG + SP + "NO EXISTEIX" + SP + MSGFI);
                            out.flush();
                        }

                        break;
                    case SELECT: {

                        while (st.hasMoreTokens()) {
                            String tmp = st.nextToken();
                            if (tmp.equals(IDMSG)) {
                                idMsg = Integer.parseInt(st.nextToken());
                            }
                        }

                        String resultSearch = dbSelect(idMsg);

                        if (!resultSearch.isEmpty()) {
                            out.println(RESULT + SP + MSG + SP + resultSearch + SP + MSGFI);
                            out.flush();
                        } else {
                            out.println(RESULT + SP + MSG + SP + "NO TROBAT" + SP + MSGFI);
                            out.flush();
                        }

                        break;
                    }
                    case SELECTALL: {

                        String resultSearch = dbSelectAll();

                        if (!resultSearch.isEmpty()) {
                            out.println(RESULT + SP + MSG + SP + resultSearch + SP + MSGFI);
                            out.flush();
                        } else {
                            out.println(RESULT + SP + MSG + SP + "NO TROBAT" + SP + MSGFI);
                            out.flush();
                        }

                        break;
                    }
                }

            } else {
                out.println(RETORNCTRL + SP + MSG + SP + "El SERVER retorna el control de les comunicacions al CLIENT" + SP + MSGFI);
                out.flush();
            }

        } catch (Exception e) {
            System.err.println(ERROR + " " + e.getMessage());
        }

        return clientMessage.contains(TANCARCONNEXIO);
    }

    private boolean dbInsert(int id, String msg) {

        Predicate p = new Predicate<Missatge>() {
            @Override
            public boolean match(Missatge m) {
                return m.getId() == id;
            }
        };

        ObjectSet<Missatge> result = db.query(p);

        if (result.size() < 1) {
            db.store(new Missatge(id, msg));
            return true;
        } else {
            return false;
        }

    }

    private boolean dbUpdate(int id, String msg) {

        Predicate p = new Predicate<Missatge>() {
            @Override
            public boolean match(Missatge m) {
                return m.getId() == id;
            }
        };

        ObjectSet<Missatge> result = db.query(p);

        if (result.size() == 1) {
            Missatge m = result.get(0);
            m.setMissatge(msg);
            db.store(m);
            return true;
        } else {
            return false;
        }

    }

    private boolean dbDelete(int id) {

        Predicate p = new Predicate<Missatge>() {
            @Override
            public boolean match(Missatge m) {
                return m.getId() == id;
            }
        };

        ObjectSet<Missatge> result = db.query(p);

        if (result.size() == 1) {
            Missatge m = result.get(0);
            db.delete(m);
            return true;
        } else {
            return false;
        }

    }

    private String dbSelect(int id) {

        Predicate p = new Predicate<Missatge>() {
            @Override
            public boolean match(Missatge m) {
                return m.getId() == id;
            }
        };

        ObjectSet<Missatge> result = db.query(p);

        if (result.size() == 1) {
            Missatge m = result.get(0);
            return m.toString();
        } else {
            return "";
        }
    }

    private String dbSelectAll() {

        Predicate p = new Predicate<Missatge>() {
            @Override
            public boolean match(Missatge m) {
                return true;
            }
        };

        ObjectSet<Missatge> result = db.query(p);

        if (result.size() >= 1) {
            StringBuilder msgList = new StringBuilder();

            for (Missatge msg : result) {
                msgList.append(msg).append(LS);
            }

            for (int i = 0; i < LS_SIZE; i++) {
                msgList.deleteCharAt(msgList.length() - 1);
            }

            return msgList.toString();
        } else {
            return "";
        }

    }

    private void closeClient(Socket clientSocket) {
        //Si falla el tancament no podem fer gaire cosa, només enregistrar el problema.
        try {
            //Tancament de tots els recursos.
            if (clientSocket != null && !clientSocket.isClosed()) {
                if (!clientSocket.isInputShutdown()) {
                    clientSocket.shutdownInput();
                }
                if (!clientSocket.isOutputShutdown()) {
                    clientSocket.shutdownOutput();
                }
                clientSocket.close();
            }
        } catch (IOException ex) {
            //Enregistrem l’error amb un objecte Logger.
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }


    public static void main(String[] args) {
        Server server = new Server();

        File dbDir = new File(DBDIR);

        if (!dbDir.exists()) {
            dbDir.mkdirs();
        }

        db = Db4oEmbedded.openFile(DB);

        server.listen();
    }

}
