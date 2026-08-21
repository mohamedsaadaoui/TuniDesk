package com.saadaoui.tunidesk.audit;

public final class AuditConstants {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_PASSWORD_RESET_REQUESTED = "PASSWORD_RESET_REQUESTED";
    public static final String ACTION_PASSWORD_RESET = "PASSWORD_RESET";

    public static final String ENTITY_USER = "USER";
    public static final String ENTITY_TICKET = "TICKET";
    public static final String ENTITY_TICKET_CATEGORY = "TICKET_CATEGORY";
    public static final String ENTITY_SLA_POLICY = "SLA_POLICY";
    public static final String ENTITY_COMMENT = "COMMENT";

    private AuditConstants() {
    }

}
