package com.stock.batch.global.utils;




import jakarta.servlet.http.HttpServletRequest;

import java.io.*;
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
            File destinationFile = new File(FilePath, Zip_Entry.getName());
            String canonicalDestinationFile = destinationFile.getCanonicalPath();
            String canonicalDestinationDirectory = Destination_Directory.getCanonicalPath();

            if (!canonicalDestinationFile.startsWith(canonicalDestinationDirectory + File.separator)) {
                throw new IOException("Entry is outside of the target dir: " + Zip_Entry.getName());
            }

            String File_Path = destinationFile.getAbsolutePath();
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



}
