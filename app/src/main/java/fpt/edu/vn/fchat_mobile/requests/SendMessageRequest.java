package fpt.edu.vn.fchat_mobile.requests;

public class SendMessageRequest {
    private String senderID;
    private String chatID;
    private String text;

    public SendMessageRequest(String senderID, String chatID, String text) {
        this.senderID = senderID;
        this.chatID = chatID;
        this.text = text;
    }

    public String getSenderID() {
        return senderID;
    }

    public void setSenderID(String senderID) {
        this.senderID = senderID;
    }

    public String getChatID() {
        return chatID;
    }

    public void setChatID(String chatID) {
        this.chatID = chatID;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
    
    @Override
    public String toString() {
        return "SendMessageRequest{" +
                "senderID='" + senderID + '\'' +
                ", chatID='" + chatID + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
