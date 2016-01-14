import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;


public class Test {
	public static void main(String[] args) {
		Test a = new Test();
		int[][] edges = {{1,0},{1,2},{1,3}};
		System.out.println(a.findMinHeightTrees(4, edges));
	}
	
    public List<Integer> findMinHeightTrees(int n, int[][] edges) {
        Set<Integer>[] adj = new HashSet[n];
        for(int i = 0;i < n;i++)
            adj[i] = new HashSet<Integer>();
        
        for(int[] edge : edges){
            adj[edge[0]].add(edge[1]);
            adj[edge[1]].add(edge[0]);
        }
        
        int max[] = {0}, curr, next = -1;
        List<Integer> longest = new ArrayList<Integer>(), res = new ArrayList<Integer>();
        Stack<Integer> stack = new Stack<Integer>();
        for(int i = 0;i < n;i++){
            if(adj[i].size() == 1){
                stack.push(i);
                dfs(stack, max, adj, longest);
                break;
            }
        }
        if(longest.isEmpty()){
            res.add(0);
            return res;
        }
        // dfs(longest.get(longest.size()-1), -1, max, new LinkedList<Integer>(), adj, path);
        stack.clear();
        stack.push(longest.get(longest.size()-1));
        dfs(stack, max, adj, longest);
        res.add(longest.get(longest.size()/2));
        if(longest.size() % 2 == 0)
            res.add(longest.get(longest.size()/2-1));        
        return res;
    }
    
    private void dfs(Stack<Integer> stack, int[] max, Set<Integer>[] adj, List<Integer> longest){
        List<Integer> path = new ArrayList<Integer>();
        Set<Integer> visited = new HashSet<Integer>();
        while(!stack.isEmpty()){
            int curr = stack.peek();
            if(visited.contains(curr)){
                stack.pop();
                if(adj[curr].size() == 1 && path.size() > max[0]){
                    max[0] = path.size();
                    longest.clear();
                    longest.addAll(path);
                }
                path.remove(path.size()-1);
            }
            else{
                visited.add(curr);
                path.add(curr);
                for(int next : adj[curr])
                    if(!visited.contains(next))
                        stack.push(next);
            }
        }
        // path.add(curr);
        // boolean isLeaf = true;
        // for(int next : adj[curr]){
        //     if(next != prev){
        //         isLeaf = false;
        //         dfs(next, curr, max, path, adj, longestPath);
        //     }
        // }
        
        // if(isLeaf && path.size() > max[0]){
        //     max[0] = path.size();
        //     longestPath.clear();
        //     longestPath.addAll(path);
        // }
        // path.remove(path.size()-1);
    }
}
