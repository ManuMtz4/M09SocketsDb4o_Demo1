package client;

import sun.misc.BASE64Decoder;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client Socket DB4o
 *
 * @author Manuel Martinez
 *         Copyright 2017, ManuMtz
 */

public class Client {

    private BufferedReader in;
    private PrintStream out;

    private static final String SP = "$##$";

    private static final String LS = System.lineSeparator();
    private static final int LS_SIZE = LS.length();

    private static final String ERROR = "ERROR";

    private static final String TANCARCONNEXIO = "TANCARCONNEXIO";
    private static final String RETORNCTRL = "RETORNCTRL";

    private static final String RESULT = "RESULT";

    private static final String INSERT = "INSERT";
    private static final String DELETE = "DELETE";
    private static final String UPDATE = "UPDATE";
    private static final String SELECT = "SELECT";
    private static final String SELECTALL = "SELECTALL";

    private static final String IDMSG = "IDMSG";
    private static final String MSG = "MSG";
    private static final String MSGFI = "MSGFI";


    private boolean noTancar = true;

    private void connect(String address, int port) {
        String serverData;
        boolean continueConnected = true;
        Socket socket;

        try {
            socket = new Socket(InetAddress.getByName(address), port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintStream(socket.getOutputStream());

            //El client atén el port fins que decideix finalitzar.
            while (continueConnected) {

                StringBuilder missatge = new StringBuilder();
                String trosMissatgeTmp;

                while (((trosMissatgeTmp = in.readLine()) != null)) {
                    missatge.append(trosMissatgeTmp).append(LS);
                    if (trosMissatgeTmp.contains(MSGFI)) {
                        break;
                    }
                }

                if (missatge.toString().length() > LS_SIZE) {
                    for (int i = 0; i < LS_SIZE; i++) {
                        missatge.deleteCharAt(missatge.length() - 1);
                    }
                }

                serverData = missatge.toString();

                //Processament de les dades rebudes i obtenció d’una nova petició.
                continueConnected = getRequest(serverData);
            }

            close(socket);
        } catch (UnknownHostException ex) {
            System.err.println("Error de connexió. No existeix el host - " + ex.getMessage());
        } catch (IOException ex) {
            System.err.println("Error de connexió indefinit - " + ex.getMessage());
        }
    }

    private boolean getRequest(String serverData) {

        noTancar = processData(serverData);

        if (!noTancar) {
            return false;
        }

        menu();

        return noTancar;
    }

    private void menu() {
        String opcio;
        Scanner sc = new Scanner(System.in);

        System.out.println();
        System.out.println("---------------- CLIENT ----------------");
        System.out.println();
        System.out.println("0. Desconnectar-se del SERVER");
        System.out.println("1. Insertar missatge");
        System.out.println("2. Insertar missatge de mès linias");
        System.out.println("3. Actualitzar missatge per ID");
        System.out.println("4. Actualitzar missatge mès linias per ID");
        System.out.println("5. Borrar missatge per ID");
        System.out.println("6. Ver un missatge per ID");
        System.out.println("7. Ver missatges");
        System.out.println();
        System.out.print("opció?: ");
        opcio = sc.nextLine();

        System.out.println();

        int id;
        String missatge;
        int nLineas;
        StringBuilder msg;

        switch (opcio) {
            case "0":
                out.println(TANCARCONNEXIO + SP + MSG + SP + "El CLIENT tanca la comunicació" + SP + MSGFI);
                out.flush();
                noTancar = false;
                break;
            case "1":
                try {
                    System.out.print("ID del missatge: ");
                    id = Integer.parseInt(sc.nextLine());

                    System.out.print("Nou missatge: ");
                    missatge = sc.nextLine();

                    out.println(INSERT + SP + IDMSG + SP + id + SP + MSG + SP + missatge + SP + MSGFI);
                    out.flush();
                } catch (Exception e) {
                    System.err.println(ERROR + " " + e.getMessage());
                    menu();
                }
                break;
            case "2":
                try {
                    System.out.print("ID del missatge: ");
                    id = Integer.parseInt(sc.nextLine());

                    System.out.print("Cuantes linies: ");
                    nLineas = Integer.parseInt(sc.nextLine());

                    System.out.print("Nou missatge: ");
                    msg = new StringBuilder();

                    System.out.println();

                    for (int i = 0; i < nLineas; i++) {
                        System.out.print("linia " + (i + 1) + ": ");
                        String text = sc.nextLine();
                        msg.append(text).append(LS);
                    }

                    if (msg.toString().length() > LS_SIZE) {
                        for (int i = 0; i < LS_SIZE; i++) {
                            msg.deleteCharAt(msg.length() - 1);
                        }
                    }

                    StringTokenizer msgTokenizer = new StringTokenizer(msg.toString(), LS);

                    out.println(INSERT + SP + IDMSG + SP + id + SP + MSG + SP);
                    out.flush();

                    while (msgTokenizer.hasMoreTokens()) {
                        out.println(msgTokenizer.nextToken(LS));
                        out.flush();
                    }

                    out.println(SP + MSGFI);
                    out.flush();
                } catch (Exception e) {
                    System.err.println(ERROR + " " + e.getMessage());
                    menu();
                }
                break;
            case "3":
                try {
                    System.out.print("ID del missatge a actualitzar: ");
                    id = Integer.parseInt(sc.nextLine());

                    System.out.print("Nou missatge: ");
                    missatge = sc.nextLine();

                    out.println(UPDATE + SP + IDMSG + SP + id + SP + MSG + SP + missatge + SP + MSGFI);
                    out.flush();
                } catch (Exception e) {
                    System.err.println(ERROR + " " + e.getMessage());
                    menu();
                }
                break;
            case "4":
                try {
                    System.out.print("ID del missatge a actualitzar: ");
                    id = Integer.parseInt(sc.nextLine());

                    System.out.print("Cuantes linies: ");
                    nLineas = Integer.parseInt(sc.nextLine());

                    System.out.print("Nou missatge: ");
                    msg = new StringBuilder();

                    System.out.println();

                    for (int i = 0; i < nLineas; i++) {
                        System.out.print("linia " + (i + 1) + ": ");
                        String text = sc.nextLine();
                        msg.append(text).append(LS);
                    }

                    if (msg.toString().length() > LS_SIZE) {
                        for (int i = 0; i < LS_SIZE; i++) {
                            msg.deleteCharAt(msg.length() - 1);
                        }
                    }

                    StringTokenizer msgTokenizer = new StringTokenizer(msg.toString(), LS);

                    out.println(UPDATE + SP + IDMSG + SP + id + SP + MSG + SP);
                    out.flush();

                    while (msgTokenizer.hasMoreTokens()) {
                        out.println(msgTokenizer.nextToken(LS));
                        out.flush();
                    }

                    out.println(SP + MSGFI);
                    out.flush();
                } catch (Exception e) {
                    System.err.println(ERROR + " " + e.getMessage());
                    menu();
                }
                break;
            case "5":
                try {
                    System.out.print("ID del missatge a borrar: ");
                    id = Integer.parseInt(sc.nextLine());

                    out.println(DELETE + SP + IDMSG + SP + id + SP + MSGFI);
                    out.flush();
                } catch (Exception e) {
                    System.err.println(ERROR + " " + e.getMessage());
                    menu();
                }
                break;
            case "6":
                try {
                    System.out.print("ID del missatge: ");
                    id = Integer.parseInt(sc.nextLine());

                    out.println(SELECT + SP + IDMSG + SP + id + SP + MSGFI);
                    out.flush();
                } catch (Exception e) {
                    System.err.println(ERROR + " " + e.getMessage());
                    menu();
                }
                break;
            case "7":
                out.println(SELECTALL + SP + MSGFI);
                out.flush();
                break;
            default:
                menu();
        }
    }

    private boolean processData(String serverData) {
        try {
            if (!serverData.isEmpty()) {

                String msg = "";

                StringTokenizer st = new StringTokenizer(serverData, SP);
                String tipusMissatge = st.nextToken(SP);

                if (tipusMissatge.equals(RESULT) || tipusMissatge.equals(RETORNCTRL)) {

                    while (st.hasMoreTokens()) {
                        String tmp = st.nextToken();
                        if (tmp.equals(MSG)) {
                            msg = st.nextToken();
                        }
                    }

                    System.out.println(msg);

                }

            }

        } catch (Exception e) {
            System.err.println(ERROR + " " + e.getMessage());
        }

        return !serverData.contains(TANCARCONNEXIO);
    }

    private void close(Socket socket) {
        //Si falla el tancament no podem fer gaire cosa, només enregistrar el problema.
        try {
            //Tancament de tots els recursos.
            if (socket != null && !socket.isClosed()) {
                if (!socket.isInputShutdown()) {
                    socket.shutdownInput();
                }
                if (!socket.isOutputShutdown()) {
                    socket.shutdownOutput();
                }
                socket.close();
            }
        } catch (IOException ex) {
            //Enregistrem l’error amb un objecte Logger.
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        Client cliente = new Client();
        cliente.connect("127.0.0.1", 9090);
    }

}
