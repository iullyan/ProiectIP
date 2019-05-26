package controller.servlets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


// Import required java libraries
import Model.dao.Test;
import Model.dao.storage.testCaseList;

import java.io.*;
import javax.servlet.annotation.WebServlet;
import javax.tools.*;


import javax.servlet.*;
import javax.servlet.http.*;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Scanner;
import java.io.*;

import java.util.ArrayList;

//import testCaseList;





// Extend HttpServlet class
@WebServlet(name = "getSolution")
public class SendSolution extends HttpServlet {

    //gasirea path-ului arhivei war
    public String getPath(String directoryname, String fileName) throws UnsupportedEncodingException {
        String path = this.getClass().getClassLoader().getResource("").getPath();
        String fullPath = URLDecoder.decode(path, "UTF-8");
        //String pathArr[] = fullPath.split("/classes/" + "");
        String pathArr[] = fullPath.split("/WEB-INF/classes/");
        System.out.println(fullPath);
        System.out.println(pathArr[0]);
        fullPath = pathArr[0];
        String reponsePath = "";
// to read a file from webcontent
        reponsePath = new File(fullPath).getPath() + File.separatorChar + directoryname + "/" + fileName;
        return reponsePath;
    }

    private String message;

    private HashMap<Integer,Boolean> rezultat = new HashMap<Integer,Boolean>();
    private String eroareCompilare;

    testCaseList testCases = new testCaseList();

    public void init()  {
        // Do required initialization

    }

    private boolean compareOutput(String s1, String s2){
        return s1.trim().equals(s2.trim());
    }




    public HashMap<Integer,Boolean> rezultateTestCaseuri (String filename , testCaseList testCases) throws IOException{

        String path= getPath("compilerScripts", "Docker") + "/" + filename;
        // Use relative path for Unix systems
        File f = new File(path);

        rezultat = new HashMap<Integer,Boolean>();

        f.getParentFile().mkdirs();
        f.createNewFile();


        FileWriter fileWriter;
        PrintWriter printWriter;
        System.out.println("teste : " + testCases.getTestCount());
        for(int i = 0 ; i < testCases.getTestCount(); i++){
            fileWriter = new FileWriter(f);
            printWriter = new PrintWriter(fileWriter);
            printWriter.print(testCases.getTestInput(i));
            printWriter.close();

            Process tempProcess = Runtime.getRuntime().exec("bash " + getPath("compilerScripts", "sandbox.sh" ) + " " +
                    getPath("compilerScripts", ""));


            String compilerOutput = "";

            BufferedReader stdout = new BufferedReader(new InputStreamReader(tempProcess.getInputStream()));

            //asteptam finalizarea procesului creat

            try{
                tempProcess.waitFor();
            }
            catch(Exception e){
                return rezultat;
            }

            //luam outputul de la proces
            String s = null;
            while ((s = stdout.readLine()) != null){
                compilerOutput = compilerOutput + s + "<br>";
            }

            stdout.close();


            if(compilerOutput.contains("error")==true){ //daca a fost eroare la compilare
                eroareCompilare = compilerOutput.substring(compilerOutput.indexOf("main.cpp"),compilerOutput.lastIndexOf((int)'^')+1);

                rezultat.put(-1,false);
                return rezultat;
            }


            BufferedReader br = new BufferedReader(new FileReader(getPath("compilerScripts","Docker") + "/sandbox/" + "output.txt"));
            String st =  br.readLine();
            System.out.println("OutputFromSabdbox : " + st + " " + st.length());
            System.out.println("OutputFromTestList : " + testCases.getTestOutput(i) + " " + testCases.getTestOutput(i).length());

            br.close();

            if(compareOutput(st, testCases.getTestOutput(i) ) == true ) rezultat.put(testCases.getTestId(i),true) ;
            else rezultat.put(testCases.getTestId(i),false);
        }

        return rezultat;

    }






    public String getEroareCompilare(){

        return eroareCompilare;

    }


    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

          int problemId = Integer.parseInt(request.getParameter("solvedProblemId"));
        System.out.println("am primit id-ul" + problemId);
          testCases= Test.getProblemTests(problemId);

        // Set response content type
        response.setContentType("text/html");

        PrintWriter out = response.getWriter();
        String cod= request.getParameter("solutionText");

        // punem codul din textbox in fisierul main.cpp

        PrintWriter scriitor = new PrintWriter(getPath("compilerScripts", "Docker") + "/" + "main.cpp");

        scriitor.println(cod);

        scriitor.close();




        String docType =
                "<!doctype html public \"-//w3c//dtd html 4.0 " + "transitional//en\">\n";

        //int val = generareScor("suma.in");

        HashMap<Integer, Boolean> result = rezultateTestCaseuri("input.txt", testCases);


        String stringResult = "";

        for (Integer i : result.keySet()) {
            stringResult = stringResult + "key: " + i + " value: " + result.get(i) + "<br>";
        }





        if(result.get(-1)==null)
            out.println(docType +
                    "<html>\n" +
                    "<body>\n" +
                    "<p>" + "Fisier compilat cu succes. " + "<br>" + stringResult + "</p>"  +
                    "</body>" +
                    "</html>"
            );


        else

            out.println(docType +
                    "<html>\n" +
                    "<body>\n" +
                    "<p>" + "Eroare compilare <br>" + eroareCompilare + "</p>" + "\n" +
                    "</body>" +
                    "</html>"
            );


        out.close();

    }


    public void destroy() {
        // do nothing.
    }
}
