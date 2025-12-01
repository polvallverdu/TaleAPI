package dev.polv.taleapi.permission;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.polv.taleapi.entity.TalePlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Default JSON file-based permission provider.
 * <p>
 * This provider stores permissions in JSON files, one per player UUID.
 * It's suitable for small to medium servers and provides a working
 * permission system out of the box.
 * </p>
 *
 * <h2>File Structure</h2>
 * <pre>
 * permissions/
 * ├── players/
 * │   ├── {uuid}.json
 * │   └── ...
 * └── groups/
 *     ├── default.json
 *     ├── vip.json
 *     └── admin.json
 * </pre>
 *
 * <h2>JSON Format</h2>
 * <pre>{@code
 * {
 *   "permissions": [
 *     {"key": "plots.create", "state": "ALLOW"},
 *     {"key": "plots.limit", "state": "ALLOW", "payload": 5},
 *     {"key": "cmd.*", "state": "ALLOW"}
 *   ],
 *   "groups": ["default", "vip"],
 *   "clientSynced": ["ui.admin.panel"]
 * }
 * }</pre>
 *
 * @see PermissionProvider
 */
public class DefaultPermissionProvider implements PermissionProvider {

    private static final String ID = "default";
    private static final String NAME = "Default JSON Provider";

    private final Path dataDirectory;
    private final ObjectMapper mapper;
    private final Executor asyncExecutor;

    // Cached permission trees per player
    private final Map<String, PermissionTree> playerTrees;
    // Cached client-synced permissions per player
    private final Map<String, Set<String>> clientSyncedCache;
    // Loaded group trees
    private final Map<String, PermissionTree> groupTrees;

    /**
     * Creates a new default provider with the specified data directory.
     *
     * @param dataDirectory the directory to store permission files
     */
    public DefaultPermissionProvider(Path dataDirectory) {
        this(dataDirectory, Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "TaleAPI-Permissions-IO");
            t.setDaemon(true);
            return t;
        }));
    }

    /**
     * Creates a new default provider with custom executor.
     *
     * @param dataDirectory the directory to store permission files
     * @param asyncExecutor executor for async operations
     */
    public DefaultPermissionProvider(Path dataDirectory, Executor asyncExecutor) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
        this.mapper = createMapper();
        this.playerTrees = new ConcurrentHashMap<>();
        this.clientSyncedCache = new ConcurrentHashMap<>();
        this.groupTrees = new ConcurrentHashMap<>();
    }

    private static ObjectMapper createMapper() {
        return new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public PermissionResult query(TalePlayer player, String key) {
        return query(player, key, ContextSet.EMPTY);
    }

    @Override
    public PermissionResult query(TalePlayer player, String key, ContextSet context) {
        PermissionTree tree = playerTrees.get(player.getUniqueId());
        if (tree == null) {
            return PermissionResult.UNDEFINED;
        }
        return tree.query(key, context);
    }

    @Override
    public PermissionTree getPlayerTree(TalePlayer player) {
        return playerTrees.get(player.getUniqueId());
    }

    @Override
    public CompletableFuture<Void> setPermission(TalePlayer player, PermissionNode node) {
        return CompletableFuture.runAsync(() -> {
            PermissionTree tree = playerTrees.computeIfAbsent(
                player.getUniqueId(), 
                k -> new PermissionTree()
            );
            tree.set(node);
            savePlayerSync(player);
        }, asyncExecutor);
    }

    @Override
    public CompletableFuture<Void> removePermission(TalePlayer player, String key) {
        return CompletableFuture.runAsync(() -> {
            PermissionTree tree = playerTrees.get(player.getUniqueId());
            if (tree != null) {
                tree.remove(key);
                savePlayerSync(player);
            }
        }, asyncExecutor);
    }

    @Override
    public Set<String> getClientSyncedNodes(TalePlayer player) {
        Set<String> synced = clientSyncedCache.get(player.getUniqueId());
        return synced != null ? Collections.unmodifiableSet(synced) : Collections.emptySet();
    }

    @Override
    public CompletableFuture<Void> loadPlayer(TalePlayer player) {
        return CompletableFuture.runAsync(() -> loadPlayerSync(player), asyncExecutor);
    }

    @Override
    public void unloadPlayer(TalePlayer player) {
        playerTrees.remove(player.getUniqueId());
        clientSyncedCache.remove(player.getUniqueId());
    }

    @Override
    public CompletableFuture<Void> invalidateCache(TalePlayer player) {
        return CompletableFuture.runAsync(() -> {
            unloadPlayer(player);
            loadPlayerSync(player);
        }, asyncExecutor);
    }

    @Override
    public void onEnable() {
        try {
            // Ensure directories exist
            Files.createDirectories(getPlayersDirectory());
            Files.createDirectories(getGroupsDirectory());

            // Load groups
            loadGroups();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize permission provider", e);
        }
    }

    @Override
    public void onDisable() {
        playerTrees.clear();
        clientSyncedCache.clear();
        groupTrees.clear();
    }

    private Path getPlayersDirectory() {
        return dataDirectory.resolve("players");
    }

    private Path getGroupsDirectory() {
        return dataDirectory.resolve("groups");
    }

    private Path getPlayerFile(TalePlayer player) {
        return getPlayersDirectory().resolve(player.getUniqueId() + ".json");
    }

    private void loadPlayerSync(TalePlayer player) {
        Path playerFile = getPlayerFile(player);
        PlayerData data = null;

        if (Files.exists(playerFile)) {
            try {
                data = mapper.readValue(playerFile.toFile(), PlayerData.class);
            } catch (IOException e) {
                // Log error, continue with empty permissions
                data = new PlayerData();
            }
        }

        if (data == null) {
            data = new PlayerData();
        }

        // Build player's tree by merging groups then personal permissions
        PermissionTree tree = new PermissionTree();

        // First, apply group permissions (in order, later groups override)
        if (data.groups != null) {
            for (String groupName : data.groups) {
                PermissionTree groupTree = groupTrees.get(groupName);
                if (groupTree != null) {
                    tree.merge(groupTree);
                }
            }
        }

        // Then apply personal permissions (highest priority)
        if (data.permissions != null) {
            for (PermissionData permData : data.permissions) {
                tree.set(permData.toNode());
            }
        }

        playerTrees.put(player.getUniqueId(), tree);

        // Cache client-synced nodes
        if (data.clientSynced != null && !data.clientSynced.isEmpty()) {
            clientSyncedCache.put(player.getUniqueId(), new HashSet<>(data.clientSynced));
        }
    }

    private void savePlayerSync(TalePlayer player) {
        Path playerFile = getPlayerFile(player);
        PermissionTree tree = playerTrees.get(player.getUniqueId());

        if (tree == null) {
            return;
        }

        PlayerData data = new PlayerData();
        data.permissions = new ArrayList<>();
        for (PermissionNode node : tree.getAllNodes()) {
            data.permissions.add(PermissionData.fromNode(node));
        }

        Set<String> synced = clientSyncedCache.get(player.getUniqueId());
        if (synced != null && !synced.isEmpty()) {
            data.clientSynced = new ArrayList<>(synced);
        }

        try {
            Files.createDirectories(playerFile.getParent());
            mapper.writeValue(playerFile.toFile(), data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save player permissions", e);
        }
    }

    private void loadGroups() {
        Path groupsDir = getGroupsDirectory();
        if (!Files.exists(groupsDir)) {
            return;
        }

        try (var stream = Files.list(groupsDir)) {
            stream.filter(p -> p.toString().endsWith(".json"))
                .forEach(this::loadGroupFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load permission groups", e);
        }

        // Create default group if it doesn't exist
        if (!groupTrees.containsKey("default")) {
            groupTrees.put("default", new PermissionTree());
        }
    }

    private void loadGroupFile(Path file) {
        String groupName = file.getFileName().toString().replace(".json", "");
        try {
            GroupData data = mapper.readValue(file.toFile(), GroupData.class);
            PermissionTree tree = new PermissionTree();

            if (data.permissions != null) {
                for (PermissionData permData : data.permissions) {
                    tree.set(permData.toNode());
                }
            }

            groupTrees.put(groupName, tree);
        } catch (IOException e) {
            // Log and continue
        }
    }

    /**
     * Sets a permission for a group.
     *
     * @param groupName the group name
     * @param node      the permission node
     * @return future completing when saved
     */
    public CompletableFuture<Void> setGroupPermission(String groupName, PermissionNode node) {
        return CompletableFuture.runAsync(() -> {
            PermissionTree tree = groupTrees.computeIfAbsent(groupName, k -> new PermissionTree());
            tree.set(node);
            saveGroupSync(groupName);
        }, asyncExecutor);
    }

    private void saveGroupSync(String groupName) {
        PermissionTree tree = groupTrees.get(groupName);
        if (tree == null) return;

        GroupData data = new GroupData();
        data.permissions = new ArrayList<>();
        for (PermissionNode node : tree.getAllNodes()) {
            data.permissions.add(PermissionData.fromNode(node));
        }

        Path file = getGroupsDirectory().resolve(groupName + ".json");
        try {
            Files.createDirectories(file.getParent());
            mapper.writeValue(file.toFile(), data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save group permissions", e);
        }
    }

    // JSON data classes

    private static class PlayerData {
        public List<PermissionData> permissions;
        public List<String> groups;
        public List<String> clientSynced;
    }

    private static class GroupData {
        public List<PermissionData> permissions;
        public int priority; // Higher priority groups override lower
    }

    private static class PermissionData {
        public String key;
        public String state;
        public Object payload;
        public Map<String, Object> context;

        PermissionNode toNode() {
            Tristate tristate = state != null ? Tristate.valueOf(state) : Tristate.UNDEFINED;
            ContextSet ctx = ContextSet.EMPTY;

            if (context != null && !context.isEmpty()) {
                ContextSet.Builder builder = ContextSet.builder();
                for (Map.Entry<String, Object> entry : context.entrySet()) {
                    builder.add(entry.getKey(), entry.getValue().toString());
                }
                ctx = builder.build();
            }

            return PermissionNode.builder(key)
                .state(tristate)
                .payload(payload)
                .context(ctx)
                .build();
        }

        static PermissionData fromNode(PermissionNode node) {
            PermissionData data = new PermissionData();
            data.key = node.getKey();
            data.state = node.getState().name();
            data.payload = node.getPayload();

            if (!node.getContext().isEmpty()) {
                data.context = node.getContext().asMap();
            }

            return data;
        }
    }
}

