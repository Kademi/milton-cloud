/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUISync;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author ibraheem
 */
public class Helper {

    public static final boolean checkInternet() {

        try {
            Socket socket = new Socket();
            InetSocketAddress add = new InetSocketAddress("www.google.com", 80);
            socket.connect(add, 3000);
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "The server or your internet connection could be down.", "connection test", JOptionPane.ERROR_MESSAGE);
            return false;
        }

    }

    public static String readUrl(String urlString, final String userid, final String password) throws Exception {
        System.out.println("1");
        BufferedReader reader = null;
        StringBuilder buffer = null;
        try {
            URL url = new URL(urlString);

            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userid, password.toCharArray());
                }
            });
            InputStreamReader s = new InputStreamReader(url.openStream());
     //       System.out.println("getEncoding " + s.getEncoding());
            reader = new BufferedReader(s);

            buffer = new StringBuilder();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1) {
                buffer.append(chars, 0, read);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "The server or your internet connection could be down.", "connection test", JOptionPane.ERROR_MESSAGE);

        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return buffer.toString();
    }

    public static ArrayList<String> getDataFromJson(String json) {
        ArrayList<String> list = new ArrayList<>();

        try {
            JSONArray array = new JSONArray(json);
            JSONObject jSONObject;
            for (int i = 0; i < array.length(); i++) {
                jSONObject = new JSONObject(array.get(i).toString());
                list.add((String) jSONObject.get("name"));

            }
        } catch (JSONException ex) {
            System.out.println(ex.getMessage());
        }
        return list;
    }

}
