package fayeoyaee;

import twitter4j.*;

/**
 * 
 *
 */
public class Extracter 
{
    
    /**
     * Given a hashtag query, extract tweets that contain links
     */
	public void extractTweets(String querystr) {
		try {
            // The factory instance is re-useable and thread safe.
            Twitter twitter = TwitterFactory.getSingleton();
            Query query = new Query(querystr);
            QueryResult result;
                result = twitter.search(query);
            for (Status status : result.getTweets()) {
                System.out.println("@" + status.getUser().getScreenName() + ":" + status.getText());
            }
		} catch (TwitterException e) {
			e.printStackTrace();
		}
	}

    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
    }
}
