package com.Ild_Mail.models;

import org.parboiled.common.FileUtils;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.xml.crypto.KeySelectorException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LetterIMAP {
    private UUID _letterId = UUID.randomUUID();
    private String _subject = null;
    private String _content = null;
    private Multipart _multipart = null;
    private List<File> _files = new ArrayList<File>();

    private Logger _logger = new Logger();
    private Message message = null;

    private String path = "./session/" + _letterId + "/";


    public LetterIMAP (Message message){
        this.message = message;
        ProcessMessage();
    }


    public String getSubject() {
        return this._subject;
    }

    public void setSubject(String subject) {
        this._subject = subject;
    }

    public String getContent() {
        return this._content;
    }

    public void setContent(String content) {
        this._content = content;
    }


    private void ProcessMessage(){
        try {
            this._subject = message.getSubject();
            ProcessContent(this.message.getContent());
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        finally {
            return;
        }
    }

    private void ProcessContent(Object content) throws Exception{
        if(content instanceof String){
            this._content = content.toString();
            _logger.PutLog("[MAIL] Plain Text Letter > " + this._subject + "\n" + content.toString());
        }
        else if(content instanceof Multipart){
            this._multipart = (Multipart) content;
            _logger.PutLog("[MAIL] Multipart Letter > " + this._subject);
            MultipartParse(_multipart);
        }
    }

    private void MultipartParse(Multipart multipartMessage) throws  Exception{
        int count = multipartMessage.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = multipartMessage.getBodyPart(i);
            String[] cids = bodyPart.getHeader("Content-Id");

            String count_ids_banner = "Count of content ids : " + (cids == null ? "No content id" : cids.length);
            _logger.PutLog(count_ids_banner);

            String cid="", content = "";
            if(cids != null && cids.length > 0){
                content = cids[0];

                if (content.startsWith("<") && content.endsWith(">")) {
                    cid = "cid:" + content.substring(1, content.length() - 1);
                } else {
                    cid = "cid:" + content;
                }
            }

            _logger.PutLog(content + "---" + cid + "\n" + bodyPart.getContentType() );

            if(bodyPart.isMimeType("text/plain")){
                String plain_text_banner = "[MAIL] Plain text letter : \n\t" + bodyPart.getContent();
                _logger.PutLog(plain_text_banner);
            }
            else if(bodyPart.isMimeType("text/html")){
                String html_text_banner = "[MAIL] HTML letter \n\t:" + bodyPart.getContent();
                Html2File(bodyPart.getContent().toString());
                _logger.PutLog(html_text_banner);
            }
            else if (bodyPart.isMimeType("multipart/*")) {
                Multipart part = (Multipart)bodyPart.getContent();
                MultipartParse(part);
            }
            else if (bodyPart.isMimeType("application/octet-stream")) {
                String disposition = bodyPart.getDisposition();

                String binraw_banner = "[MAIL] binary raw:" + disposition;
                _logger.PutLog(binraw_banner);

                if (disposition.equalsIgnoreCase(BodyPart.ATTACHMENT)) {
                    ProcessAttachment(bodyPart);
                }
            } else if (bodyPart.isMimeType("image/*") && !("".equals(cid))) {
                ProcessEmbeddedImage(bodyPart);
            }
        }
    }

    private void ProcessEmbeddedImage(BodyPart bodyPart) throws MessagingException, IOException {
        DataHandler dataHandler = bodyPart.getDataHandler();
        String name = dataHandler.getName();

        String image_banner = "[MAIL] embedded picture name:" + name;
        _logger.PutLog(image_banner);

        InputStream is = dataHandler.getInputStream();
        File file = new File( this.path + name);
        Save(is, new FileOutputStream(file));
    }

    private void ProcessAttachment(BodyPart bodyPart) throws MessagingException, IOException {
        String fileName = bodyPart.getFileName();
        String attach_banner = "[INFO] saving attachment" + fileName;
        _logger.PutLog(attach_banner);
        InputStream is = bodyPart.getInputStream();
        File file = new File(this.path + fileName);
        Save(is, new FileOutputStream(file));
    }

    private void Save(InputStream is, OutputStream os) throws IOException {
        byte[] bytes = new byte[1024];
        int len = 0;
        while ((len=is.read(bytes)) != -1 ) {
            os.write(bytes, 0, len);
        }
        if (os != null)
            os.close();
        if (is != null)
            is.close();
    }

    private void Html2File(String htmlContent){
        File htmlFile = new File(this.path + "mail_html.html");
        FileUtils.writeAllText(htmlContent,htmlFile);
    }
}
