package com.savereplaybufferforobs;

public interface DisplaysExceptions {
    public void setObsException(ObsException exception);

    public void clearObsException();

    /** Shows a one-off message in the game chat. */
    void showChatMessage(String message);
}
