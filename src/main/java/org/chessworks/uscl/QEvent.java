package org.chessworks.uscl;

import org.chessworks.common.validation.ValidationException;

public final class QEvent {
    private static final String SEP = " | ";
    private static final String AND = " & ";
    private static final int ALLOW_GUESTS = 1;
    private static final int NO_JOIN_WINDOW = 2;
    private static final int NO_WATCH_WINDOW = 4;
    private static final int NO_INFO_WINDOW = 8;
    private static final int MAX_FIELD_LENGTH = 319;
    private static final int MAX_GROUP_LENGTH = 99;
    private static final int MAX_COMMAND_LENGTH = 1029;

    private int eventNumber;

    private String description = "";

    /**
     * A bit-field which indicates:
     * <ul>
     * <li>(bit-field & 1) means guests can perhaps watch and/or join,</li>
     * <li>(bit-field & 2) means don't make a new window when sending the join commands,</li>
     * <li>(bit-field & 4) means don't make a new window when sending the watch commands, and</li>
     * <li>(bit-field & 8) means don't make a new window when sending the info commands.</li>
     * </ul>
     */
    private int bitField = ALLOW_GUESTS | NO_JOIN_WINDOW | NO_WATCH_WINDOW | NO_INFO_WINDOW;
    
    private final StringBuilder joinField = new StringBuilder();
    private final StringBuilder watchField = new StringBuilder();
    private final StringBuilder infoField = new StringBuilder();
    
    private String confirmText = "";
    
    private String groupName;

    private QEvent(int eventNumber) {
        this.eventNumber = eventNumber;
    }
    
    public static QEvent event(int eventNumber) {
        QEvent result = new QEvent(eventNumber);
        return result;
    }
    
    public QEvent description(String description, Object... args) {
        if (args.length > 0) {
            description = String.format(description, args);
        }
        this.description = escapeText(description);
        return this;
    }
    
    public QEvent addJoinCommand(String command, Object... args) {
        addCommand(this.joinField, command, args);
        return this;
    }

    public QEvent addJoinLink(String url) {
        addLink(this.joinField, url);
        return this;
    }

    public QEvent addWatchCommand(String command, Object... args) {
        addCommand(this.watchField, command, args);
        return this;
    }

    public QEvent addWatchLink(String url) {
        addLink(this.watchField, url);
        return this;
    }

    public QEvent addInfoCommand(String command, Object... args) {
        addCommand(this.infoField, command, args);
        return this;
    }

    public QEvent addInfoLink(String url) {
        addLink(this.infoField, url);
        return this;
    }

    private static void addCommand(StringBuilder field, String command, Object... args) {
        if (args.length > 0) {
            command = String.format(command, args);
        }
        command = trim(command);
        if (command.isEmpty())
            return;
        command = escapeText(command);
        if (field.length() > 0) {
            field.append(AND);
        }
        field.append(command);
    }
    
    private static void addLink(StringBuilder field, String url) {
        if (!url.startsWith("http")) {
            throw new IllegalArgumentException("Event urls must start with http");
        }
        addCommand(field, url);
    }
    
    public QEvent confirmationMessage(String confirmText, Object... args) {
        if (args.length > 0) {
            confirmText = String.format(confirmText, args);
        }
        this.confirmText = escapeText(confirmText);
        return this;
    }

    public QEvent restrictToGroup(String groupName) {
        groupName = trim(groupName);
        if (!groupName.isEmpty())
            this.groupName = groupName;
        else
            this.groupName = null;
        return this;
    }

    public QEvent excludeGroup(String groupName) {
        groupName = trim(groupName);
        if (!groupName.isEmpty())
            this.groupName = "!" + groupName;
        else
            this.groupName = null;
        return this;
    }

    public QEvent allowGuests(boolean allowed) {
        setBit(ALLOW_GUESTS, allowed);
        return this;
    }
    
    public QEvent newWindowForJoin(boolean newWindow) {
        setBit(NO_JOIN_WINDOW, !newWindow);
        return this;
    }
    
    public QEvent newWindowForWatch(boolean newWindow) {
        setBit(NO_WATCH_WINDOW, !newWindow);
        return this;
    }
    
    public QEvent newWindowForInfo(boolean newWindow) {
        setBit(NO_INFO_WINDOW, !newWindow);
        return this;
    }
    
    public int getEventNumber() {
        return eventNumber;
    }

    public int getBitField() {
        return bitField;
    }

    public void setBitField(int bitField) {
        this.bitField = bitField;
    }

    public String getDescription() {
        return description;
    }

    public String getJoinField() {
        return joinField.toString();
    }

    public String getWatchField() {
        return watchField.toString();
    }

    public String getInfoField() {
        return infoField.toString();
    }

    public String getGroupName() {
        return groupName.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("qaddevent ");
        builder.append(this.eventNumber);
        builder.append(" ");
        builder.append(this.bitField);
        builder.append(" ");
        builder.append(this.description);
        builder.append(SEP);
        builder.append(joinField);
        builder.append(SEP);
        builder.append(watchField);
        builder.append(SEP);
        builder.append(infoField);
        builder.append(SEP);
        builder.append(this.confirmText);
        if (groupName != null) {
            builder.append(SEP);
            builder.append(groupName);
        }
        String result = builder.toString();
        return result;
    }
    
    public QEvent validate() throws ValidationException {
        ValidationException.Builder validation = ValidationException.create();
        validation.checkLength("description", MAX_FIELD_LENGTH, this.description);
        validation.checkLength("join-commands", MAX_FIELD_LENGTH, this.joinField);
        validation.checkLength("watch-commands", MAX_FIELD_LENGTH, this.watchField);
        validation.checkLength("info-commands", MAX_FIELD_LENGTH, this.infoField);
        validation.checkLength("confirm-text", MAX_FIELD_LENGTH, this.confirmText);
        validation.checkLength("groupname", MAX_GROUP_LENGTH, this.groupName);
        String command = this.toString();
        validation.setValue(command);
        validation.checkLength("qaddevent", MAX_COMMAND_LENGTH, command);
        validation.throwIfErrorsFound();
        return this;
    }
    
    public QEvent send(USCLBot.Commands tdCommands) throws ValidationException {
        return addEvent(tdCommands);
    }

    public QEvent addEvent(USCLBot.Commands tdCommands) throws ValidationException {
        validate();
        String command = this.toString();
        tdCommands.sendAdminCommand(command);
        return this;
    }

    public QEvent removeEvent(USCLBot.Commands tdCommands) {
        tdCommands.sendAdminCommand("qremoveevent {0}", this.eventNumber);
        return this;
    }

    public static String escapeText (String text) {
        String result = trim(text);
        if (result.contains("&"))
            result=result.replace("&",  "\\&");
        if (result.contains("|"))
            result=result.replace("|",  "\\|");
        return result;
    }

    private void setBit(int bitMask, boolean value) {
        if (value) {
            this.bitField |= bitMask;
        } else {
            this.bitField &= ~bitMask;
        }
    }

    private static String trim(String s) {
        if (s==null) {
            return "";
        }
        s = s.trim();
        return s;
    }
    
}
