package dev.polv.taleapi.permission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Radix Tree (Trie) data structure for efficient permission lookups.
 * <p>
 * This hierarchical structure provides O(k) lookup time where k is the length
 * of the permission string, regardless of how many permissions exist in the
 * tree.
 * </p>
 *
 * <h2>Performance Advantages</h2>
 * <ul>
 * <li><b>Wildcards are instant:</b> If traversal hits a {@code *} node, it
 * stops immediately</li>
 * <li><b>No string parsing:</b> Permissions are naturally segmented by
 * dots</li>
 * <li><b>Organization:</b> Related permissions are grouped (cmd.teleport,
 * cmd.give)</li>
 * </ul>
 *
 * <h2>Example Structure</h2>
 * 
 * <pre>
 * root
 * ├── cmd
 * │   ├── teleport [ALLOW]
 * │   ├── give [DENY]
 * │   └── * [ALLOW]        // Wildcard: cmd.anything = ALLOW
 * └── plots
 *     ├── create [ALLOW]
 *     └── limit [ALLOW, payload=5]
 * </pre>
 *
 * <h2>Thread Safety</h2>
 * <p>
 * This implementation uses ConcurrentHashMap for thread-safe reads.
 * For bulk modifications, external synchronization is recommended.
 * </p>
 *
 * @see PermissionNode
 */
public final class PermissionTree {

  /**
   * The wildcard character that matches any permission segment.
   */
  public static final String WILDCARD = "*";

  private final TreeNode root;

  /**
   * Creates an empty permission tree.
   */
  public PermissionTree() {
    this.root = new TreeNode("");
  }

  /**
   * Creates a permission tree from a collection of nodes.
   *
   * @param nodes the permission nodes to add
   */
  public PermissionTree(Collection<PermissionNode> nodes) {
    this();
    for (PermissionNode node : nodes) {
      set(node);
    }
  }

  /**
   * Sets a permission in the tree.
   *
   * @param node the permission node to set
   */
  public void set(PermissionNode node) {
    Objects.requireNonNull(node, "node");
    String[] segments = splitKey(node.getKey());
    TreeNode current = root;

    for (String segment : segments) {
      current = current.children.computeIfAbsent(segment, TreeNode::new);
    }

    current.nodes.add(node);
  }

  /**
   * Sets a simple permission (ALLOW with no payload).
   *
   * @param key the permission key
   */
  public void allow(String key) {
    set(PermissionNode.allow(key));
  }

  /**
   * Sets a simple deny permission.
   *
   * @param key the permission key
   */
  public void deny(String key) {
    set(PermissionNode.deny(key));
  }

  /**
   * Removes all permission nodes for a key.
   *
   * @param key the permission key to remove
   * @return {@code true} if any nodes were removed
   */
  public boolean remove(String key) {
    String[] segments = splitKey(key);
    TreeNode current = root;

    for (String segment : segments) {
      TreeNode child = current.children.get(segment);
      if (child == null) {
        return false;
      }
      current = child;
    }

    boolean hadNodes = !current.nodes.isEmpty();
    current.nodes.clear();
    return hadNodes;
  }

  /**
   * Queries the tree for a permission result.
   * <p>
   * This method traverses the tree, checking for wildcards at each level.
   * If a wildcard is found, it returns immediately.
   * </p>
   *
   * @param key the permission key to query
   * @return the permission result
   */
  public PermissionResult query(String key) {
    return query(key, ContextSet.EMPTY);
  }

  /**
   * Queries the tree for a permission result with context.
   * <p>
   * This method traverses the tree segment by segment:
   * </p>
   * <ol>
   * <li>At each level, check for a wildcard (*) node</li>
   * <li>If wildcard exists and matches context, return its result</li>
   * <li>Continue to the specific segment if no wildcard match</li>
   * <li>At the final segment, return the node's result if it matches context</li>
   * </ol>
   *
   * @param key     the permission key to query
   * @param context the current context to match against
   * @return the permission result
   */
  public PermissionResult query(String key, ContextSet context) {
    Objects.requireNonNull(key, "key");
    Objects.requireNonNull(context, "context");

    String[] segments = splitKey(key);
    TreeNode current = root;
    PermissionResult wildcardResult = null;

    for (int i = 0; i < segments.length; i++) {
      // Check for wildcard at this level first
      TreeNode wildcardNode = current.children.get(WILDCARD);
      if (wildcardNode != null) {
        PermissionResult result = findMatchingResult(wildcardNode, context);
        if (result != null && result.getState().isDefined()) {
          // Store the most specific wildcard match
          wildcardResult = result;
        }
      }

      // Navigate to the specific segment
      TreeNode child = current.children.get(segments[i]);
      if (child == null) {
        // No more specific path - return wildcard result if we have one
        return wildcardResult != null ? wildcardResult : PermissionResult.UNDEFINED;
      }
      current = child;
    }

    // We've reached the exact node - check for a result
    PermissionResult exactResult = findMatchingResult(current, context);
    if (exactResult != null && exactResult.getState().isDefined()) {
      return exactResult;
    }

    // Check for wildcard child (e.g., "cmd.teleport.*" when querying
    // "cmd.teleport")
    TreeNode wildcardChild = current.children.get(WILDCARD);
    if (wildcardChild != null) {
      PermissionResult result = findMatchingResult(wildcardChild, context);
      if (result != null && result.getState().isDefined()) {
        return result;
      }
    }

    // Fall back to wildcard result if no exact match
    return wildcardResult != null ? wildcardResult : PermissionResult.UNDEFINED;
  }

  /**
   * Checks if a permission is allowed (returns true for ALLOW state).
   *
   * @param key the permission key
   * @return {@code true} if the permission is ALLOW
   */
  public boolean has(String key) {
    return query(key).isAllowed();
  }

  /**
   * Checks if a permission is allowed with context.
   *
   * @param key     the permission key
   * @param context the current context
   * @return {@code true} if the permission is ALLOW
   */
  public boolean has(String key, ContextSet context) {
    return query(key, context).isAllowed();
  }

  /**
   * Gets all permission nodes in this tree.
   *
   * @return an unmodifiable list of all nodes
   */
  public List<PermissionNode> getAllNodes() {
    List<PermissionNode> all = new ArrayList<>();
    collectNodes(root, all);
    return Collections.unmodifiableList(all);
  }

  /**
   * Gets all permission keys in this tree.
   *
   * @return a list of all permission keys
   */
  public List<String> getAllKeys() {
    List<String> keys = new ArrayList<>();
    collectKeys(root, "", keys);
    return keys;
  }

  /**
   * Returns the number of permission nodes in this tree.
   *
   * @return the total node count
   */
  public int size() {
    return countNodes(root);
  }

  /**
   * Checks if this tree is empty.
   *
   * @return {@code true} if no permissions are set
   */
  public boolean isEmpty() {
    return root.children.isEmpty() && root.nodes.isEmpty();
  }

  /**
   * Clears all permissions from this tree.
   */
  public void clear() {
    root.children.clear();
    root.nodes.clear();
  }

  /**
   * Merges another tree into this one.
   * <p>
   * Nodes from the other tree are added to this tree.
   * Existing nodes with the same key are not replaced.
   * </p>
   *
   * @param other the tree to merge from
   */
  public void merge(PermissionTree other) {
    for (PermissionNode node : other.getAllNodes()) {
      set(node);
    }
  }

  /**
   * Creates a flattened tree by merging multiple trees with priority.
   * <p>
   * Trees are merged in order, with later trees having higher priority.
   * This is used for permission resolution (Personal > Group > Default).
   * </p>
   *
   * @param trees the trees to merge, in ascending priority order
   * @return a new flattened tree
   */
  public static PermissionTree flatten(PermissionTree... trees) {
    PermissionTree result = new PermissionTree();
    for (PermissionTree tree : trees) {
      if (tree != null) {
        result.merge(tree);
      }
    }
    return result;
  }

  private PermissionResult findMatchingResult(TreeNode node, ContextSet context) {
    // Find the first node that matches the context
    // Later nodes (higher priority) are checked first
    for (int i = node.nodes.size() - 1; i >= 0; i--) {
      PermissionNode permNode = node.nodes.get(i);
      if (permNode.appliesInContext(context)) {
        return permNode.toResult();
      }
    }
    return null;
  }

  private void collectNodes(TreeNode node, List<PermissionNode> result) {
    result.addAll(node.nodes);
    for (TreeNode child : node.children.values()) {
      collectNodes(child, result);
    }
  }

  private void collectKeys(TreeNode node, String prefix, List<String> keys) {
    for (PermissionNode permNode : node.nodes) {
      keys.add(permNode.getKey());
    }
    for (Map.Entry<String, TreeNode> entry : node.children.entrySet()) {
      String childPrefix = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
      collectKeys(entry.getValue(), childPrefix, keys);
    }
  }

  private int countNodes(TreeNode node) {
    int count = node.nodes.size();
    for (TreeNode child : node.children.values()) {
      count += countNodes(child);
    }
    return count;
  }

  private static String[] splitKey(String key) {
    return key.split("\\.");
  }

  /**
   * Internal tree node.
   */
  private static final class TreeNode {
    final Map<String, TreeNode> children;
    final List<PermissionNode> nodes;

    TreeNode(String segment) {
      // segment parameter kept for potential debugging/logging use
      this.children = new ConcurrentHashMap<>();
      this.nodes = Collections.synchronizedList(new ArrayList<>());
    }
  }
}
