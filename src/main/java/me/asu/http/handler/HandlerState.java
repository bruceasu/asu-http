package me.asu.http.handler;

/**
 * Created by suk on 2019/6/2.
 */
public enum HandlerState {
    READY(0), STARTED(1), SHUTTING_DOWN(2), SHUTDOWN(3),
    ;
    int value = -1;

    HandlerState(int val)
    {
        this.value = val;
    }

}
