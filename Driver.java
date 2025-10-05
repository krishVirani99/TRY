public final class Driver {
    public static void main(String[] args) {
        // Minimal, safe entry point so "java Driver" succeeds.
        // Construct the required class and exit without error.
        new InterlockingImpl();
    }
}
