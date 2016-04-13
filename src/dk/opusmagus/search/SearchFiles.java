package dk.opusmagus.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

/** Simple command-line based search demo. */
public class SearchFiles {

  private SearchFiles() {}

  /** Simple command-line based search demo. */
  public static void main(String[] args) throws Exception {

	  System.out.println("Lucene search tutorial started...");
	  
    String index = "C:\\lucene_data\\index";
    String field = "json";
    String queries = "";
    int repeat = 0;
    boolean raw = false;
    String queryString = "";
    int hitsPerPage = 10;
    
    IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(index)));
    IndexSearcher searcher = new IndexSearcher(reader);
    Analyzer analyzer = new StandardAnalyzer();
      
    //String line = "Transit Courier?Tourneo Courier";
    //String line = "\"KEY\":\"02|T6|A1|AV\",\"CATEGORY\":\"MO_06\",\"VALUE\":\"Transit Courier?Tourneo Courier\"";
    //String line = "ourneo Courier";
    //String line = "YOUcantfindmeeeeeee";
    //String line = "Mitsubishi ASX 2WD 1.8 diesel Invite 5dr OD";
    //String line = "itsubishi ASX 2WD 1.8 diesel Invite 5dr";
    //String line = "nier de roue de seco";
    //String line = "nierde roue de secu";
    String line = "318d";
    
    //MultiFieldQueryParser mparser = new MultiFieldQueryParser();
    QueryParser parser = new QueryParser(field, analyzer);    
	Query query = parser.parse(line);
	//parser.setPhraseSlop(120);
	System.out.println("Searching for: " + query.toString(field));
	        
    Date start = new Date();
    int maxHits = 20;
    TopDocs topDocs = searcher.search(query, maxHits);
    ScoreDoc[] hits = topDocs.scoreDocs;
    
    for(ScoreDoc hit : hits)
    {
	    Document doc = searcher.doc(hit.doc);
	    IndexableField pathField = doc.getField("path");
	    if (pathField != null)   
	        System.out.println("path:" + pathField);
	    
	    IndexableField contentsField = doc.getField("json");
	    if (contentsField != null)   
	        System.out.println("json:" + contentsField);
	    
	    IndexableField modifiedField = doc.getField("modified");
	    if (modifiedField != null)   
	        System.out.println("modified:" + modifiedField);	    
    }
    
    Date end = new Date();
    System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
	
	  //doPagingSearch(in, searcher, query, hitsPerPage, raw, queries == null && queryString == null);
	
	  //if (queryString != null) {
	    //break;
	  //}    
	  
	  System.out.println("Lucene search tutorial ended!");
  }

  /**
   * This demonstrates a typical paging search scenario, where the search engine presents 
   * pages of size n to the user. The user can then go to the next page if interested in
   * the next hits.
   * 
   * When the query is executed for the first time, then only enough results are collected
   * to fill 5 result pages. If the user wants to page beyond this limit, then the query
   * is executed another time and all hits are collected.
   * 
   */
  public static void doPagingSearch(BufferedReader in, IndexSearcher searcher, Query query, 
                                     int hitsPerPage, boolean raw, boolean interactive) throws IOException {
 
    // Collect enough docs to show 5 pages
    TopDocs results = searcher.search(query, 5 * hitsPerPage);
    ScoreDoc[] hits = results.scoreDocs;
    
    int numTotalHits = results.totalHits;
    System.out.println(numTotalHits + " total matching documents");

    int start = 0;
    int end = Math.min(numTotalHits, hitsPerPage);
        
    while (true) {
      if (end > hits.length) {
        System.out.println("Only results 1 - " + hits.length +" of " + numTotalHits + " total matching documents collected.");
        System.out.println("Collect more (y/n) ?");
        String line = in.readLine();
        if (line.length() == 0 || line.charAt(0) == 'n') {
          break;
        }

        hits = searcher.search(query, numTotalHits).scoreDocs;
      }
      
      end = Math.min(hits.length, start + hitsPerPage);
      
      for (int i = start; i < end; i++) {
        if (raw) {                              // output raw format
          System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
          continue;
        }

        Document doc = searcher.doc(hits[i].doc);
        String path = doc.get("path");
        if (path != null) {
          System.out.println((i+1) + ". " + path);
          String title = doc.get("title");
          if (title != null) {
            System.out.println("   Title: " + doc.get("title"));
          }
        } else {
          System.out.println((i+1) + ". " + "No path for this document");
        }
                  
      }

      if (!interactive || end == 0) {
        break;
      }

      if (numTotalHits >= end) {
        boolean quit = false;
        while (true) {
          System.out.print("Press ");
          if (start - hitsPerPage >= 0) {
            System.out.print("(p)revious page, ");  
          }
          if (start + hitsPerPage < numTotalHits) {
            System.out.print("(n)ext page, ");
          }
          System.out.println("(q)uit or enter number to jump to a page.");
          
          String line = in.readLine();
          if (line.length() == 0 || line.charAt(0)=='q') {
            quit = true;
            break;
          }
          if (line.charAt(0) == 'p') {
            start = Math.max(0, start - hitsPerPage);
            break;
          } else if (line.charAt(0) == 'n') {
            if (start + hitsPerPage < numTotalHits) {
              start+=hitsPerPage;
            }
            break;
          } else {
            int page = Integer.parseInt(line);
            if ((page - 1) * hitsPerPage < numTotalHits) {
              start = (page - 1) * hitsPerPage;
              break;
            } else {
              System.out.println("No such page");
            }
          }
        }
        if (quit) break;
        end = Math.min(numTotalHits, start + hitsPerPage);
      }
    }
  }
}
