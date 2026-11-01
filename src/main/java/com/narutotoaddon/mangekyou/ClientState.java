package com.narutotoaddon.mangekyou;

/** Estado do lado do cliente (implante atual, enviado pelo servidor). */
public final class ClientState {
    public static volatile String implant = "";
    private ClientState() {}
}
