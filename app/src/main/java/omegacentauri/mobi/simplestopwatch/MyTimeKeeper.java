package omegacentauri.mobi.simplestopwatch;

interface MyTimeKeeper {
    void updateViews();
    void restore();
    void stopUpdating();
    void destroy();
    void suspend();

    void copyToClipboard();
}
