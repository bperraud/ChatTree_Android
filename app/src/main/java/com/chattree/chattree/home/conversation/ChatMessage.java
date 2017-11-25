package com.chattree.chattree.home.conversation;

/**
 * Created by steveamadodias on 19/11/2017.
 */

public class ChatMessage {
    private String messagetext;
    private boolean fromMyself;
    private int idMessage;
    private String pseudo;

    public ChatMessage(String messagetext,boolean fromMyself,int idMessage,String pseudo){
        this.messagetext= messagetext;
        this.fromMyself = fromMyself;
        this.idMessage = idMessage;
        this.pseudo = pseudo;


    }

    public ChatMessage(String messagetext, boolean fromMyself){
        this(messagetext,fromMyself,-1, "unknown");
    }

    public String getMessagetext(){
        return messagetext;
    }

    public boolean isFromMyself(){
        return fromMyself;
    }

    public String getPseudo(){
        return pseudo;
    }

}
