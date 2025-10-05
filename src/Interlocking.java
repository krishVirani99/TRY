public interface Interlocking {

    // Add a train by name at an entry section, with an intended exit section.
    boolean addTrain(String trainName, int entrySection, int exitSection);

    // Move the listed trains once. Return how many actually moved (usually 1).
    Integer moveTrains(String[] trainNames);

    // Train ID currently in this section, or null if empty.
    String getSection(int section);

    // Current section of this train, or -1 if it has exited/not present.
    int getTrain(String trainName);
}
