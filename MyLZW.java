/******************************************************************************
 *  Compilation:  javac MyLZW.java
 *  Execution:    java MyLZW - < input.txt   (compress)
 *  Execution:    java MyLZW + < input.txt   (expand)
 *  Dependencies: BinaryIn.java BinaryOut.java
 *  Data files:   http://algs4.cs.princeton.edu/55compression/abraLZW.txt
 *                http://algs4.cs.princeton.edu/55compression/ababLZW.txt
 *
 *  Compress or expand binary input from standard input using LZW.
 *
 *  WARNING: STARTING WITH ORACLE JAVA 6, UPDATE 7 the SUBSTRING
 *  METHOD TAKES TIME AND SPACE LINEAR IN THE SIZE OF THE EXTRACTED
 *  SUBSTRING (INSTEAD OF CONSTANT SPACE AND TIME AS IN EARLIER
 *  IMPLEMENTATIONS).
 *
 *  See <a href = "http://java-performance.info/changes-to-string-java-1-7-0_06/">this article</a>
 *  for more details.
 *
 ******************************************************************************/

import edu.princeton.cs.algs4.BinaryStdIn;
import edu.princeton.cs.algs4.BinaryStdOut;
import edu.princeton.cs.algs4.TST;
import java.util.*;
import java.lang.Math.*;

/**
 *  The {@code MyLZW} class provides static methods for compressing
 *  and expanding a binary input using LZW compression over the 8-bit extended
 *  ASCII alphabet with 12-bit codewords.
 *  <p>
 *  For additional documentation,
 *  see <a href="http://algs4.cs.princeton.edu/55compress">Section 5.5</a> of
 *  <i>Algorithms, 4th Edition</i> by Robert Sedgewick and Kevin Wayne.
 *
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */
public class MyLZW {
    private static final int min_bits = 9;
    private static final int max_bits = 16;
    private static final int R = 256;       // number of input chars
    private static int L = 512;             //initial max number of codewords = 2^W
    private static int W = min_bits;        //initial codeword width
    private static int mode;


    // Do not instantiate.
    private MyLZW() { }

    /**
     * Reads a sequence of 8-bit bytes from standard input; compresses
     * them using LZW compression with 12-bit codewords; and writes the results
     * to standard output.
     */
    public static void compress() {
        double file_size = 0;
        double compress_data = 0;
        double compress_ratio = 0;

        String input = BinaryStdIn.readString();
        file_size = input.length();

        TST<Integer> st = resetCodeBook();
        int code = R+1;  // R is codeword for EOF

        BinaryStdOut.write(mode, W);
        if(mode == 2)
            BinaryStdOut.write(file_size);

        while (input.length() > 0) {
            String s = st.longestPrefixOf(input);  // Find max prefix match s.
            BinaryStdOut.write(st.get(s), W);      // Print s's encoding.
            int t = s.length();
            compress_data += (W/8);

            if (t < input.length()){    //check end of string not reached
                if(code < L)           //check if codeword width needs to be incremented
                    st.put(input.substring(0, t + 1), code++);
                else{
                    //check if codeword bit length can be resized
                    if(W < max_bits){
                        resize(W + 1);
                        st.put(input.substring(0, t + 1), code++);
                    }
                    else if(mode == 1){   //check if codebook needs to be reset
                        
                        st = resetCodeBook();
                        code = R + 1;
                        resize(min_bits);
                        st.put(input.substring(0, t + 1), code++);
                    }
                    else if(mode == 2){     //check if monitor mode is on
                        if(compress_ratio == 0){
                            compress_ratio = file_size/compress_data;
                            
                        }
                        else{
                            double current_ratio = file_size/compress_data;
                            if((compress_ratio/current_ratio) > 1.1){
                                
                                compress_ratio = file_size/compress_data;
                                st = resetCodeBook();
                                code = R + 1;
                                resize(min_bits);
                                st.put(input.substring(0, t + 1), code++);
                            }
                        }
                    }
                }
            }
            input = input.substring(t);            // Scan past s in input.
        }
        BinaryStdOut.write(R, W);
        BinaryStdOut.close();
    }

    /**
     * Reads a sequence of bit encoded using LZW compression with
     * 12-bit codewords from standard input; expands them; and writes]
     * the results to standard output.
     */
    public static void expand() {
        double file_size = 0;
        double compress_data = 0;
        double compress_ratio = 0;
        String[] st = new String[(int)Math.pow(2,max_bits)];
        int i; // next available codeword value

        // initialize symbol table with all 1-character strings
        for (i = 0; i < R; i++){
            st[i] = "" + (char) i;
        }
        st[i++] = "";                        // (unused) lookahead for EOF

        mode = BinaryStdIn.readInt(W);
        if(mode == 2)
            file_size = BinaryStdIn.readDouble();

        int codeword = BinaryStdIn.readInt(W);
        if (codeword == R) return;           // expanded message is empty string
        String val = st[codeword];
        compress_data += (W/8);

        while (true) {
            if(i >= L){
                if(W < max_bits){
                    resize(W + 1);
                }
                else if(mode == 1){
                    
                    resize(min_bits);
                    st = new String[(int)Math.pow(2,max_bits)];
                    for (i = 0; i < R; i++)
                    {
                        st[i] = "" + (char) i;
                    }
                    st[i++] = "";
                }
                else if(mode == 2){
                    if(compress_ratio == 0){
                        
                        compress_ratio = file_size/compress_data;
                    }
                    else{
                        double current_ratio = file_size/compress_data;
                        if((compress_ratio/current_ratio) > 1.1){
                            
                            compress_ratio = file_size/compress_data;
                            resize(min_bits);
                            st = new String[(int)Math.pow(2,max_bits)];
                            for (i = 0; i < R; i++)
                            {
                                st[i] = "" + (char) i;
                            }
                            st[i++] = "";
                        }
                    }
                }
            }
            BinaryStdOut.write(val);
            codeword = BinaryStdIn.readInt(W);
            compress_data += (W/8);
            if (codeword == R) break;
            String s = st[codeword];
            if (i == codeword) s = val + val.charAt(0);   // special case hack
            if (i < L) st[i++] = val + s.charAt(0);
            val = s;
        }
        BinaryStdOut.close();
    }

    public static TST<Integer> resetCodeBook(){
        TST<Integer> st = new TST<Integer>();
        for (int i = 0; i < R; i++)
            st.put("" + (char) i, i);
        return st;
    }

    public static void resize(int new_width){
        W = new_width;
        L = (int)Math.pow(2, W);
        
    }

    /**
     * Sample client that calls {@code compress()} if the command-line
     * argument is "-" an {@code expand()} if it is "+".
     *
     * @param args the command-line arguments
     */
    public static void main(String[] args) {
        if(args[0].equals("-")) 
        {
            String mode_choice = args[1];
            if(mode_choice.equals("n"))
            {
                mode = 0;
            }
            else if(mode_choice.equals("r"))
            {
                mode = 1;
            }
            else if(mode_choice.equals("m"))
            {
                mode = 2;
            }
            compress();
        }
        else if (args[0].equals("+")) 
        {
            expand();
        }
        else 
        {
            throw new IllegalArgumentException("Illegal command line argument");
        }
    }

}

/******************************************************************************
 *  Copyright 2002-2016, Robert Sedgewick and Kevin Wayne.
 *
 *  This file is part of algs4.jar, which accompanies the textbook
 *
 *      Algorithms, 4th edition by Robert Sedgewick and Kevin Wayne,
 *      Addison-Wesley Professional, 2011, ISBN 0-321-57351-X.
 *      http://algs4.cs.princeton.edu
 *
 *
 *  algs4.jar is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  algs4.jar is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with algs4.jar.  If not, see http://www.gnu.org/licenses.
 ******************************************************************************/
