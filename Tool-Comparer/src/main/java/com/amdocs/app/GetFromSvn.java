package com.amdocs.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNProperties;
import org.tmatesoft.svn.core.SVNProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;


public class GetFromSvn {
	
	public String discoverPath(SVNRepository repository, String fileName, String path)
			throws SVNException {

		String tempPath;
		String result;
		DAVRepositoryFactory.setup();

		try {
			Collection<SVNDirEntry> entries = repository.getDir(path, -1, null,(Collection) null);
			Iterator<SVNDirEntry> iterator = entries.iterator();
			
			while (iterator.hasNext()) {
				SVNDirEntry entry = iterator.next();
				
				if(entry.getKind().equals(SVNNodeKind.DIR)) {

					tempPath = this.getConcatenatePath(path, entry.getName());
					
					result = this.discoverPath(repository, fileName, tempPath);
					if(result != null) {
						return result;
					}
				} else if(entry.getKind().equals(SVNNodeKind.FILE)){
					if(entry.getName().equals(fileName)) {
						return this.getConcatenatePath(path, entry.getName());
					}
				}
			}
			return null;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

	}
	
	private String getConcatenatePath(String path, String dir) {
		String concatenatedPath = path;
		if(path.length() > 1) {
			concatenatedPath = concatenatedPath + "/";
		}
		concatenatedPath = concatenatedPath + dir;
		return concatenatedPath;
	}

    public static void main(String[] args) {
    	
    	GetFromSvn gfs = new GetFromSvn(); //TODO: find an appropriate class name.
    	
    	ResourceBundle resourceConfig = ResourceBundle.getBundle("properties");

        String url = resourceConfig.getString("stageReport");
        String name = resourceConfig.getString("svnusername");
        String password = resourceConfig.getString("svnpass");
        String fileName = resourceConfig.getString("fileName");
        String filePath;

        setupLibrary();

        SVNRepository repository = null;
        try {
            /*
             * Creates an instance of SVNRepository to work with the repository.
             * All user's requests to the repository are relative to the
             * repository location used to create this SVNRepository.
             * SVNURL is a wrapper for URL strings that refer to repository locations.
             */
            repository = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(url));
        } catch (SVNException svne) {
            /*
             * Perhaps a malformed URL is the cause of this exception
             */
            System.err
                    .println("error while creating an SVNRepository for the location '"
                            + url + "': " + svne.getMessage());
            System.exit(1);
        }

        /*
         * User's authentication information (name/password) is provided via  an 
         * ISVNAuthenticationManager  instance.  SVNWCUtil  creates  a   default 
         * authentication manager given user's name and password.
         * 
         * Default authentication manager first attempts to use provided user name 
         * and password and then falls back to the credentials stored in the 
         * default Subversion credentials storage that is located in Subversion 
         * configuration area. If you'd like to use provided user name and password 
         * only you may use BasicAuthenticationManager class instead of default 
         * authentication manager:
         * 
         *  authManager = new BasicAuthenticationsManager(userName, userPassword);
         *  
         * You may also skip this point - anonymous access will be used. 
         */
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(name, password);
        repository.setAuthenticationManager(authManager);

        /*
         * This Map will be used to get the file properties. Each Map key is a
         * property name and the value associated with the key is the property
         * value.
         */
        SVNProperties fileProperties = new SVNProperties();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            /*
             * Checks up if the specified path really corresponds to a file. If
             * doesn't the program exits. SVNNodeKind is that one who says what is
             * located at a path in a revision. -1 means the latest revision.
             */
            
            filePath = gfs.discoverPath(repository,fileName,"");
        	System.out.println(filePath);
        	
        	SVNNodeKind nodeKind = repository.checkPath(filePath, -1);
            
           
            if (nodeKind == SVNNodeKind.NONE) {
                System.err.println("There is no entry at '" + url + "'.");
                System.exit(1);
            } else if (nodeKind == SVNNodeKind.DIR) {
                System.err.println("The entry at '" + url
                        + "' is a directory while a file was expected.");
                System.exit(1);
            }
            
            /*
             * Gets the contents and properties of the file located at filePath
             * in the repository at the latest revision (which is meant by a
             * negative revision number).
             */
            repository.getFile(filePath, -1, fileProperties, baos);

        } catch (SVNException svne) {
            System.err.println("error while fetching the file contents and properties: " + svne.getMessage());
            System.exit(1);
        }

        /*
        
        /*
         * Here the SVNProperty class is used to get the value of the
         * svn:mime-type property (if any). SVNProperty is used to facilitate
         * the work with versioned properties.
         */
        String mimeType = fileProperties.getStringValue(SVNProperty.MIME_TYPE);

        /*
         * SVNProperty.isTextMimeType(..) method checks up the value of the mime-type
         * file property and says if the file is a text (true) or not (false).
         */
        boolean isTextType = SVNProperty.isTextMimeType(mimeType);

        Iterator iterator = fileProperties.nameSet().iterator();
        /*
         * Displays file properties.
         */
        while (iterator.hasNext()) {
            String propertyName = (String) iterator.next();
            String propertyValue = fileProperties.getStringValue(propertyName);
            System.out.println("File property: " + propertyName + "="
                    + propertyValue);
        }
        /*
         * Displays the file contents in the console if the file is a text.
         */
        if (isTextType) {
            System.out.println("File contents:");
            System.out.println();
            try {
                baos.writeTo(System.out);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            System.out
                    .println("File contents can not be displayed in the console since the mime-type property says that it's not a kind of a text file.");
        }
        /*
         * Gets the latest revision number of the repository
         */
        long latestRevision = -1;
        try {
            latestRevision = repository.getLatestRevision();
        } catch (SVNException svne) {
            System.err.println("error while fetching the latest repository revision: " + svne.getMessage());
            System.exit(1);
        }
        System.out.println("");
        System.out.println("---------------------------------------------");
        System.out.println("Repository latest revision: " + latestRevision);
        System.exit(0);
        
    }

    /*
     * Initializes the library to work with a repository via 
     * different protocols.
     */
    private static void setupLibrary() {
        /*
         * For using over http:// and https://
         */
        DAVRepositoryFactory.setup();
        /*
         * For using over svn:// and svn+xxx://
         */
        SVNRepositoryFactoryImpl.setup();
        
        /*
         * For using over file:///
         */
        FSRepositoryFactory.setup();
    }
}