package com.stock.batch.utils;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CommonUtils {

    public static String getConcatList(List<String> list ){
        StringBuffer sb = new StringBuffer();

        for(String item : list ){
            sb.append(item);
            sb.append(",");
        }
        sb.setLength(sb.length()-1);

        return sb.toString();
    }


    public static void unZip(String ZipFilePath, String FilePath) throws IOException{
        File Destination_Directory = new File(FilePath);
        if (!Destination_Directory.exists()) {
            Destination_Directory.mkdir();
        }

            ZipInputStream Zip_Input_Stream = new ZipInputStream(new FileInputStream(ZipFilePath));
            ZipEntry Zip_Entry = Zip_Input_Stream.getNextEntry();

            while (Zip_Entry != null) {
                String File_Path = FilePath + File.separator + Zip_Entry.getName();
                if (!Zip_Entry.isDirectory()) {

                    extractFile(Zip_Input_Stream, File_Path);
                } else {

                    File directory = new File(File_Path);
                    directory.mkdirs();
                }
                Zip_Input_Stream.closeEntry();
                Zip_Entry = Zip_Input_Stream.getNextEntry();
            }
            Zip_Input_Stream.close();
    }

    private static void extractFile(ZipInputStream Zip_Input_Stream, String File_Path) throws IOException {
        int BUFFER_SIZE = 4096;

        BufferedOutputStream Buffered_Output_Stream = new BufferedOutputStream(new FileOutputStream(File_Path));
        byte[] Bytes = new byte[BUFFER_SIZE];
        int Read_Byte = 0;
        while ((Read_Byte = Zip_Input_Stream.read(Bytes)) != -1) {
            Buffered_Output_Stream.write(Bytes, 0, Read_Byte);
        }
        Buffered_Output_Stream.close();
    }

    public static String getRequestRemoteIp(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip.contains(",")) {
            String[] remoteIpParts = ip.split(",");
            ip = remoteIpParts[0].trim();
        }
        return ip;
    }

    public static List<String> parseLocdatesFromXml(String xml) throws Exception {
        List<String> locdates = new ArrayList<>();

        if(StringUtils.hasText(xml)){
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
            Document doc = builder.parse(input);
            doc.getDocumentElement().normalize();

            //오류 응답 확인
            NodeList errorHeaders = doc.getElementsByTagName("cmmMsgHeader");
            if (errorHeaders.getLength() > 0) {
                Element error = (Element) errorHeaders.item(0);
                String errMsg = getTagValue("errMsg", error);
                String returnAuthMsg = getTagValue("returnAuthMsg", error);
                throw new RuntimeException("API 오류: " + errMsg + " - " + returnAuthMsg);
            }

            //item 리스트 추출 (없을 수도 있음)
            NodeList itemList = doc.getElementsByTagName("item");
            for (int i = 0; i < itemList.getLength(); i++) {
                Element item = (Element) itemList.item(i);
                String locdate = getTagValue("locdate", item);
                if (!locdate.isEmpty()) {
                    locdates.add(locdate);
                }
            }
        }
        return locdates;
    }

    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0 && nodeList.item(0).getFirstChild() != null) {
            return nodeList.item(0).getFirstChild().getNodeValue();
        }
        return "";
    }

}
