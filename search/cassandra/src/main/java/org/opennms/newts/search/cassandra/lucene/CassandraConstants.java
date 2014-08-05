package org.opennms.newts.search.cassandra.lucene;


class CassandraConstants {

    // Tables
    static String T_FILES = "lucene_files";
    static String T_FILES_INDEX = "lucene_files_idx";
    static String T_LOCKS = "lucene_locks";

    // Attributes (T_FILES)
    static String C_FILES_INDEX = "index_name";
    static String C_FILES_ID = "id";
    static String C_FILES_SEGMENT = "segment";
    static String C_FILES_DATA = "data";

    // Attributes (T_FILES_INDEX)
    static String C_FILES_INDEX_INDEX = "index_name";
    static String C_FILES_INDEX_NAME = "name";
    static String C_FILES_INDEX_ID = "id";

    // Attributes (T_LOCKS)
    static String C_LOCKS_NAME = "name";
    static String C_LOCKS_LOCKED = "locked";

}
