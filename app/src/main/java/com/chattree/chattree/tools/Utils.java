package com.chattree.chattree.tools;

import com.chattree.chattree.db.User;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

public class Utils {

    /**
     * Converts the contents of an InputStream to a String.
     */
    static public String readStream(InputStream stream, int maxReadSize) throws IOException {
        Reader        reader    = new InputStreamReader(stream, "UTF-8");
        char[]        rawBuffer = new char[maxReadSize];
        int           readSize;
        StringBuilder buffer    = new StringBuilder();
        while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
            if (readSize > maxReadSize) {
                readSize = maxReadSize;
            }
            buffer.append(rawBuffer, 0, readSize);
            maxReadSize -= readSize;
        }
        return buffer.toString();
    }

    /**
     * @param member The member we want the label of
     * @return The label of the member, deduced from its info
     */
    static public String getLabelFromUser(User member) {
        String res;
        if (member.getLogin() != null)
            res = member.getLogin();
        else if (member.getFirstname() != null && member.getLastname() != null)
            res = member.getFirstname() + " " + member.getLastname();
        else if (member.getFirstname() != null)
            res = member.getFirstname();
        else if (member.getLastname() != null)
            res = member.getLastname();
        else
            res = member.getEmail();
        return res;
    }
}
