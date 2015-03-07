package com.android.internal.telephony;


interface ITelephony {
    boolean endCall();
    void answerRingingCall();
    void silenceRinger();
}
