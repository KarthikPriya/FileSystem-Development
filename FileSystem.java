import java.util.Scanner;

//Operating System Project Submission - File System Development
//Code by Karthik Priya Narayanasamy 
public class FileSystem{
	DirectoryStructure directoryStructure;
	UserData userData;
	static enum blockUsage{
		FREE, 
		DIRECTORY,
		USERDATA,
		INVALID;
	}
	static enum fileOpenMode{
		INPUT,
		OUTPUT,
		UPDATE,
		INVALID;
	}
	private blockUsage blockUsageType;
	public static FileSystem[] fileSystem;
	public static fileOpenMode openFileMode = fileOpenMode.INVALID;
	public static int openFileBlockNumber = -1;
	public static int openFilesDirectory = -1;
	public static int openFileSeekPosition = -1;
	public static final int TOTAL_BLOCKS = 100;
	public static final int MAX_USERDATA_BLOCK_SIZE = 504;
	public static final int MAX_ENTRIES_IN_A_DIR = 32;
	
	
	FileSystem()
	{
		directoryStructure = new DirectoryStructure();
		userData = new UserData();
		blockUsageType = blockUsage.FREE;
	}

	public DirectoryStructure getDirectoryStructure() {
		return directoryStructure;
	}

	public void setDirectoryStructure(DirectoryStructure directoryStructure) {
		this.directoryStructure = directoryStructure;
	}
	
	public UserData getUserData() {
		return userData;
	}

	public void setUserData(UserData userData) {
		this.userData = userData;
	}

	// TODO modify bytes in last block in the directory content
	public static void main(String args[])
	{
		//Creating actual blocks
		fileSystem = new FileSystem[FileSystem.TOTAL_BLOCKS];
		for (int loop = 0; loop < FileSystem.TOTAL_BLOCKS; loop++)
		{
			fileSystem[loop] = new FileSystem();
		}
        fileSystem[0].setBlockUsageType(FileSystem.blockUsage.DIRECTORY);
		String userInput;
		Scanner scanner = new Scanner(System.in);
		do
		{
			fileSystem[0].getDirectoryStructure().setFree(getFirstFreeBlock());
			System.out.println("\nEnter the command (help for Help | print to print Block statistics | exit to Exit):");
			userInput = scanner.nextLine();
			// System.out.println("User input: " + userInput);
			// validate user input
			String operation = userInput.split(" ")[0];
			if (operation == null)
			{
				System.out.println("Invalid UserInput " + userInput);
				continue;
			}

			switch(operation.toLowerCase())
			{
				case "create":
					if (!operationCreate(userInput))
					{
						System.out.println("Invalid input for create: " + userInput);
					}
					break;
				case "delete":
					if (!operationDelete(userInput))
					{
						System.out.println("Invalid input for delete: " + userInput);
					}
					break;
				case "open":
					if(!operationOpen(userInput))
					{
						System.out.println("Invalid input for open: " + userInput);
					}
					break;
				case "read":
					if (openFileBlockNumber == -1)
					{
						System.out.println("Please open the file before reading");
					}
					else if (openFileMode != FileSystem.fileOpenMode.INPUT && openFileMode != FileSystem.fileOpenMode.UPDATE)
					{
                        System.out.println("Please open the file in INPUT or UPDATE mode for reading");						
					}
					else if (!operationRead(userInput))
					{
						System.out.println("Invalid input for read " + userInput);
					}
					break;
				case "write":
					if (openFileBlockNumber == -1)
					{
						System.out.println("Please open the file before writing");
					}
					else if (openFileMode != FileSystem.fileOpenMode.OUTPUT && openFileMode != FileSystem.fileOpenMode.UPDATE)
					{
                        System.out.println("Please open the file in OUTPUT or UPDATE mode for writing");						
					}
					else if (!operationWrite(userInput))
					{
						System.out.println("Invalid input for write " + userInput);
					}
					break;
				case "seek":
					if (openFileBlockNumber == -1)
					{
						System.out.println("Please open the file before seeking");
					}
					else if (openFileMode != FileSystem.fileOpenMode.INPUT && openFileMode != FileSystem.fileOpenMode.UPDATE)
					{
                        System.out.println("Please open the file in INPUT or UPDATE mode for seeking");						
					}
					else if (!operationSeek(userInput))
					{
						System.out.println("Invalid input for write " + userInput);
					}
					break;
				case "close":
					if (openFileBlockNumber == -1)
					{
						System.out.println("Please create/open the file before closing");
					}
					else if (userInput.equalsIgnoreCase("close"))
					{
						openFileBlockNumber = -1;
						openFileMode = FileSystem.fileOpenMode.INVALID;
						openFileSeekPosition = -1;
						openFilesDirectory = -1;
					}
					else
					{
						System.out.println("Invalid input for close " + userInput);
					}
					break;
				case "exit":
					System.out.println("Exiting the program");
					break;
				case "print":
					System.out.println("<------------------- Printing Statistics ------------------->");
					printBlocksStatistics();
					break;
				case "help":
					printHelp();
					break;
				default:
					System.out.println("Invalid input: " + userInput);
			}
		}while(!userInput.equalsIgnoreCase("exit"));
		scanner.close();
	}
	
	private static void printHelp() {
		System.out.println("<------------- Commands supported --------->");
		System.out.println("    CREATE D|U DirectoryName|FileName  -- To create a directory or file");
		System.out.println("    OPEN I|O|U FileName                -- To open a file in Input|Output|Update mode");
		System.out.println("    CLOSE                              -- To close a file");
		System.out.println("    DELETE DirectoryName|FileName      -- To delete a directory|file");
		System.out.println("    READ n                             -- To read n bytes from previously opened file");
		System.out.println("    WRITE n 'data'                     -- To write n bytes of data to previously opened or created file");
		System.out.println("    SEEK -1|0|1 offset                 -- To seek in a file");
		System.out.println("<-------------- END ----------------------->");
	}

	private static boolean operationSeek(String userInput) {
		String[] data = userInput.split(" ");
		if (data.length != 3)
		{
			return false;
		}
		int base;
		int offset;
		try {
			base = Integer.parseInt(data[1]);
			offset = Integer.parseInt(data[2]);
		}
		catch (NumberFormatException e)
		{
			System.out.println("Please provide both base and offset as numbers");
			return false;
		}
		if (base < -1 || base > 1)
		{
			System.out.println("seek base should be either -1, 0 or 1");
			return false;
		}
		else if (base == -1 && offset < 0)
		{
			System.out.println("Negative offset not supported with beggining of the file");
			return false;
		}
		else if (base == 0)
		{
			if ((openFileSeekPosition - offset) < 0)
			{
				System.out.println("Seek failed.. File pointer going before index 0");
				return false;
			}
		}
		else if (base == 1 && offset > 0)
		{
			System.out.println("Positive offset not supported with end of the file");
			return false;
		}
		
		int tmpBlockNumber = openFileBlockNumber;
		String existingData = "";
		while (tmpBlockNumber != 0)
		{
			existingData += fileSystem[tmpBlockNumber].getUserData().getUserData();
			tmpBlockNumber = fileSystem[tmpBlockNumber].getUserData().getForward();
		}
		
		switch (base)
		{
		    case -1:
		    	if (offset > existingData.length())
		    	{
		    		System.out.println("Seek failed.. File pointer going beyond the length of the existing data");
		    		System.out.println("Length of existing data: " + existingData.length());
		    		return false;
		    	}
		    	else
		    	{
		    		openFileSeekPosition = offset;
		    	}
			    break;
		    case 0:
		    	if (offset + openFileSeekPosition > existingData.length())
		    	{
		    		System.out.println("Seek failed.. File pointer going beyond the length of the existing data");
		    		System.out.println("Length of existing data: " + existingData.length());
		    		return false;
		    	}
		    	openFileSeekPosition += offset;
			    break;
		    case 1:
		    	if (offset + existingData.length() < 0)
		    	{
		    		System.out.println("Seek failed.. File pointer is less than index 0");
		    		System.out.println("Length of existing data: " + existingData.length());
		    		return false;
		    	}
		    	openFileSeekPosition = offset + existingData.length();
			    break;
		}
		return true;
	}


	private static boolean operationWrite(String userInput) {
		String[] data = userInput.split(" ");
		if (data.length != 3)
		{
			return false;
		}
		int numberOfBytesToWrite = 0;
		try {
			numberOfBytesToWrite = Integer.parseInt(data[1]);
		}
		catch (NumberFormatException e)
		{
			System.out.println("Please provide bytes in number");
			return false;
		}
		String actualData = data[2];
		if (data[2].startsWith("'") && data[2].endsWith("'"))
		{
			actualData = data[2].substring(1, data[2].length() - 1);
		}
		
		if (numberOfBytesToWrite > actualData.length())
		{
			String tempBlank = new String(new char[numberOfBytesToWrite - actualData.length()]).replace('\0', ' ');
			actualData += tempBlank;
		}
		else if (numberOfBytesToWrite < actualData.length())
		{
			actualData = actualData.substring(0, numberOfBytesToWrite);
		}
		System.out.println("Actual data to be written: " + actualData + "$END$");
		if (fileSystem[openFileBlockNumber].getBlockUsageType() != blockUsage.USERDATA)
		{
			System.out.println("Software error while writing .. block type not USERDATA");
			return false;
		}

		int tmpBlockNumber = openFileBlockNumber;
		int actualDataPos = 0;
		String existingData = "";
		String dataToBeWritten = "";
		while (tmpBlockNumber != 0)
		{
			existingData += fileSystem[tmpBlockNumber].getUserData().getUserData();
			tmpBlockNumber = fileSystem[tmpBlockNumber].getUserData().getForward();
		}
		//System.out.println("Open File seek pos: " + openFileSeekPosition + " existingData: " + existingData);
		if (openFileSeekPosition == -1)
		{
			System.out.println("Software error please check the code. data not written");
			return false;
		}
		else if (openFileSeekPosition == 0 && existingData.length() == 0)
		{
			dataToBeWritten = actualData;
			actualDataPos = actualData.length();
			//dataToBeWritten = existingData.substring(0, openFileSeekPosition) + actualData + existingData.substring(openFileSeekPosition);
		}
		else if (openFileSeekPosition == 0 && existingData.length() != 0)
		{
			if (actualData.length() <= existingData.length())
			{
			    dataToBeWritten = actualData + existingData.substring(actualData.length());
			    actualDataPos = actualData.length();
			}
			else
			{
				dataToBeWritten = actualData;
				actualDataPos = actualData.length();
			}
		}
		else if (openFileSeekPosition == existingData.length())
		{
			dataToBeWritten = existingData + actualData;
			actualDataPos = existingData.length() + actualData.length();
		}
		else
		{
			dataToBeWritten = existingData.substring(0, openFileSeekPosition) + actualData;
			if (openFileSeekPosition < existingData.length()) {
				dataToBeWritten += existingData.substring(openFileSeekPosition);
			}
			actualDataPos = openFileSeekPosition + actualData.length();
		}
        int dataToBeWrittenLength = dataToBeWritten.length();
        tmpBlockNumber = openFileBlockNumber;
        openFileSeekPosition = 0;
        int dataWrittenSuccessfully = 0;
        while (dataToBeWrittenLength > 0)
        {
        	if (dataToBeWrittenLength >= MAX_USERDATA_BLOCK_SIZE)
        	{
        	    fileSystem[tmpBlockNumber].getUserData().setUserData(dataToBeWritten.substring(dataWrittenSuccessfully, dataWrittenSuccessfully + MAX_USERDATA_BLOCK_SIZE));
        	    dataWrittenSuccessfully += MAX_USERDATA_BLOCK_SIZE;
        	    fileSystem[openFilesDirectory].getDirectoryStructure().UpdateLastFreeBytes(openFileBlockNumber, 0);
        	}
        	else
        	{
        		fileSystem[tmpBlockNumber].getUserData().setUserData(dataToBeWritten.substring(dataWrittenSuccessfully));
        		dataWrittenSuccessfully += dataToBeWrittenLength; 
        		fileSystem[openFilesDirectory].getDirectoryStructure().UpdateLastFreeBytes(openFileBlockNumber, MAX_USERDATA_BLOCK_SIZE - dataToBeWrittenLength);
        	}
            if (dataWrittenSuccessfully >= actualDataPos)
            {
            	openFileSeekPosition = actualDataPos;
            }
        	dataToBeWrittenLength -= MAX_USERDATA_BLOCK_SIZE;

        	if (dataToBeWrittenLength > 0)
        	{
        		if(fileSystem[tmpBlockNumber].getUserData().getForward() == 0)
        		{
        			int firstFreeBlock = getFirstFreeBlock();
    				if (firstFreeBlock == -1)
    				{
    					System.out.println("DISK FULL");
    					if (openFileSeekPosition == 0)
    					{
    						openFileSeekPosition = dataWrittenSuccessfully;
    					}
    					return true;
    				}
    				else
    				{
    					fileSystem[firstFreeBlock].setBlockUsageType(blockUsage.USERDATA);
    					fileSystem[firstFreeBlock].getUserData().setForward(0);
    					fileSystem[firstFreeBlock].getUserData().setBack(tmpBlockNumber);
    					fileSystem[tmpBlockNumber].getUserData().setForward(firstFreeBlock);
    					tmpBlockNumber = firstFreeBlock;
    				}
        		}
        		else
        		{
        			tmpBlockNumber = fileSystem[tmpBlockNumber].getUserData().getForward();
        		}
        	}
        }
        if (openFileSeekPosition == 0)
        {
        	System.out.println("Software error in write." + getLineNumber());
        }
    	return true;
	}

	public static int getLineNumber() {
	    return Thread.currentThread().getStackTrace()[2].getLineNumber();
	}

	private static boolean operationRead(String userInput) {
		String[] data = userInput.split(" ");
		if (data.length != 2)
		{
			return false;
		}
		int numberOfBytesToRead = 0;
		try {
			numberOfBytesToRead = Integer.parseInt(data[1]);
		}
		catch (NumberFormatException e)
		{
			System.out.println("Please provide bytes in number");
			return false;
		}
		if (openFileBlockNumber < 0 || openFileBlockNumber >= FileSystem.TOTAL_BLOCKS)
		{
			System.out.println("Some issue with open and create. openfileblocknumber is " + openFileBlockNumber);
			return false;
		}
		int tmpBlockNumber = openFileBlockNumber;
		int endIndex;
		String userDataString = "";
		while (tmpBlockNumber != 0)
		{
			userDataString += fileSystem[tmpBlockNumber].getUserData().getUserData();
			tmpBlockNumber = fileSystem[tmpBlockNumber].getUserData().getForward();
		}
		System.out.println("OpenFileSeek Pos: " + openFileSeekPosition + " data: " + userDataString);
        if ((openFileSeekPosition + numberOfBytesToRead) >= userDataString.length())
        {
        	System.out.println("");
        	endIndex = userDataString.length();
        }
        else
        {
        	endIndex = openFileSeekPosition + numberOfBytesToRead;
        }
        System.out.println("<---------File Content --------->");
        System.out.println(userDataString.substring(openFileSeekPosition, endIndex));
        if ((openFileSeekPosition + numberOfBytesToRead) > userDataString.length())
        {
        	System.out.println("End of the file reached before reading " + numberOfBytesToRead + " bytes");
        }
        openFileSeekPosition = endIndex;
        System.out.println("OpenFileSeek Pos: " + openFileSeekPosition);
		return true;
	}


	private static boolean operationOpen(String userInput) {
		String[] data = userInput.split(" ");
		if (data.length != 3)
		{
			return false;
		}
		switch(data[1].toUpperCase())
		{
				case "I":
					openFileMode = FileSystem.fileOpenMode.INPUT;
					break;
				case "O":
					openFileMode = FileSystem.fileOpenMode.OUTPUT;
					break;
				case "U":
					openFileMode = FileSystem.fileOpenMode.UPDATE;
					break;
				default:
					System.out.println("Invalid Mode specified.. Please check input");
					return false;
						
		}
		return openFileSpecified(data[2]);
	}


	private static boolean openFileSpecified(String absoluteName) {
		String[] subnames = absoluteName.split("/");
		int blockNumber = 0;
		int pos = 0;
		//System.out.println("Debug: filename to open: " + subnames[subnames.length - 1]);
		while(pos < subnames.length)
		{
			if (pos + 1 == subnames.length)
		    {
				int tmpBlockNumber;
            	do {
            		tmpBlockNumber = fileSystem[blockNumber].getDirectoryStructure().checkifFileExistsInSpecifiedBlock(subnames[pos]);
            		if (tmpBlockNumber != -1 || fileSystem[blockNumber].getDirectoryStructure().getFrwd() == 0)
            		{
            			break;
            		}
            		else
            		{
            			blockNumber = fileSystem[blockNumber].getDirectoryStructure().getFrwd();
            		}
            	}while (true);
         
            	if (tmpBlockNumber == -1)
			    {
            		System.out.println("Filename " + subnames[pos] + " doesn't exists");
            		return false;
			    }
            	else
            	{
            		openFileBlockNumber = tmpBlockNumber;
            		openFilesDirectory = blockNumber;
            		if (openFileMode == fileOpenMode.INPUT || openFileMode == fileOpenMode.UPDATE)
            		{
            			openFileSeekPosition = 0;
            		}
            		else if (openFileMode == fileOpenMode.OUTPUT)
            		{
            			String existingData = "";
            			while (tmpBlockNumber != 0)
            			{
            				existingData += fileSystem[tmpBlockNumber].getUserData().getUserData();
            				tmpBlockNumber = fileSystem[tmpBlockNumber].getUserData().getForward();
            			}
            			openFileSeekPosition = existingData.length();
            		}
            		else
            		{
            			System.out.println("SOFTWARE ERROR.. FILEOPENMODE INVALID LineNumber: " + getLineNumber());
            			return false;
            		} 
            	}
		    }
			else
			{
				int tmpBlockNumber;
            	do {
            		tmpBlockNumber = fileSystem[blockNumber].getDirectoryStructure().checkifDirExistsInSpecifiedBlock(subnames[pos]);
            		if (tmpBlockNumber != -1 || fileSystem[blockNumber].getDirectoryStructure().getFrwd() == 0)
            		{
            			break;
            		}
            		else
            		{
            			blockNumber = fileSystem[blockNumber].getDirectoryStructure().getFrwd();
            		}
            	}while (true);
         
            	if (tmpBlockNumber == -1)
			    {
            		System.out.println("Directory " + subnames[pos] + " doesn't exists");
            		return false;
			    }
            	blockNumber = tmpBlockNumber;
			}
			pos++;
		}
		return true;
	}


	public static boolean operationCreate(String userInput)
	{
		String[] data = userInput.split(" ");
		if (data.length != 3)
		{
			return false;
		}
		if (data[1].equals("U") || data[1].equals("D"))
		{
			System.out.println("Creating Directory or UserData");
		    return createFileOrDirectory(data[1], data[2]);
		}
		else
			return false;
	}
	
	public static boolean operationDelete(String userInput)
	{
		String[] data = userInput.split(" ");
		if (data.length != 2)
		{
			return false;
		}
		System.out.println("Deleting Directory or UserData");
		return deleteFileOrDirectory(data[1]);
	}
	
	private static boolean deleteFileOrDirectory(String absoluteName) {
		//System.out.println("FileOrDir to delete: " + absoluteName);
		String[] subnames = absoluteName.split("/");
		int blockNumber = 0;
		int pos = 0;

		while(pos < subnames.length)
		{
		    if (pos + 1 == subnames.length)
		    {
		    	int tmpBlockNumber;
		    	FileSystem.blockUsage tmpBlockType = FileSystem.blockUsage.INVALID;
		    	int backBlockNumber = -1;
		    	do {
            		tmpBlockNumber = fileSystem[blockNumber].getDirectoryStructure().checkifDirOrFileExistsInSpecifiedBlockAndGetLink(subnames[pos]);
            		if (tmpBlockNumber != -1 || fileSystem[blockNumber].getDirectoryStructure().getFrwd() == 0)
            		{
            			if (tmpBlockNumber != -1)
            			{
            				tmpBlockType = fileSystem[blockNumber].getDirectoryStructure().checkifDirOrFileExistsInSpecifiedBlockAndGetType(subnames[pos]);
            			}
            			break;
            		}
            		else
            		{
            			backBlockNumber = blockNumber;
            			blockNumber = fileSystem[blockNumber].getDirectoryStructure().getFrwd();
            		}
            	}while (true);
		    	
		    	if (tmpBlockNumber == -1)
		    	{
		    		System.out.println("File or directory doesn't exists in the file system " + subnames[pos]);
		    		return false;
		    	}
		    	else
		    	{
		    		if ((fileSystem[tmpBlockNumber].getBlockUsageType() == blockUsage.DIRECTORY) && (!fileSystem[tmpBlockNumber].getDirectoryStructure().IsDirectoryEmpty()))
		    		{
		    			System.out.println("Directory requested to delete not empty");
		    			return false;
		    		}
		    		else if (fileSystem[tmpBlockNumber].getBlockUsageType() == blockUsage.USERDATA)
		    		{
		    			int frwdTmpBlockNumber = fileSystem[tmpBlockNumber].getUserData().getForward();
		    			while (frwdTmpBlockNumber != 0)
		    			{
		    				int tmpFrwdBlockNumber = frwdTmpBlockNumber;
		    				frwdTmpBlockNumber = fileSystem[tmpFrwdBlockNumber].getUserData().getForward();
		    				resetBlock(tmpFrwdBlockNumber);
		    			}
		    		}
		    		resetBlock(tmpBlockNumber);
		    		fileSystem[blockNumber].getDirectoryStructure().deleteAnEntryInDirectoryContents(subnames[pos]);
		    		if (backBlockNumber != -1)
		    		{
		    			if (fileSystem[blockNumber].getDirectoryStructure().IsDirectoryEmpty())
		    			{
		    				fileSystem[backBlockNumber].getDirectoryStructure().setFrwd(0);
		    				resetBlock(blockNumber);
		    			}
		    		}
		    	}
		    }
		    else
		    {
		    	int tmpBlockNumber;
		    	do {
            		tmpBlockNumber = fileSystem[blockNumber].getDirectoryStructure().checkifDirExistsInSpecifiedBlock(subnames[pos]);
            		if (tmpBlockNumber != -1 || fileSystem[blockNumber].getDirectoryStructure().getFrwd() == 0)
            		{
            			break;
            		}
            		else
            		{
            			blockNumber = fileSystem[blockNumber].getDirectoryStructure().getFrwd();
            		}
            	}while (true);
		    	if (tmpBlockNumber == -1)
		    	{
		    		System.out.println("Sub Directory " + subnames[pos] + " doesn't exists in the file system ");
		    		return false;
		    	}
		    	blockNumber = tmpBlockNumber;
		    }
		    pos ++;
		}
		return true;
	}


	private static boolean createFileOrDirectory(String type, String absoulteName) {
		String[] subnames = absoulteName.split("/");
		int blockNumber = 0;
		int pos = 0;
		blockUsage blockType = blockUsage.INVALID;
		int openedFileBlockNumber = -1;

		while(pos < subnames.length)
		{
		    if (pos + 1 == subnames.length)
		    {
		    	// TODO check if file already exists and delete and replace
		    	int alreadyExistsBlockNumber = fileSystem[blockNumber].getDirectoryStructure().checkifDirOrFileExistsInSpecifiedBlockAndGetLink(subnames[pos]);
		    	if (alreadyExistsBlockNumber != -1)
		    	{
		    		blockUsage alreadyExistingBlockType = fileSystem[blockNumber].getDirectoryStructure().checkifDirOrFileExistsInSpecifiedBlockAndGetType(subnames[pos]);
		    		if (alreadyExistingBlockType == blockUsage.DIRECTORY)
		    		{
		    		    System.out.println("A directory with the same name already exists in the same location");
		    		    return false;
		    		}
		    		else if (alreadyExistingBlockType == blockUsage.USERDATA)
		    		{
		    			//printBlocksStatistics();
		    			System.out.println("A file with the same name already exists in the same location.. Deleting and recreating file/directory");
		    			if(!operationDelete("delete " + absoulteName)) 
		    			{
		    				System.out.println("Software error while deleting already existing file " + getLineNumber());
		    				return false;
		    			}
		    		}
		    	}
		    	int firstFreeBlock = getFirstFreeBlock();
		    	if (firstFreeBlock == -1)
		    	{
		    		System.out.println("NO MORE FREE BLOCKS");
		    		printBlocksStatistics();
		    		return false;
		    	}
		    	switch(type)
		    	{
		    	    case "U":
		    	    	fileSystem[firstFreeBlock].setBlockUsageType(blockUsage.USERDATA);
		    	    	fileSystem[firstFreeBlock].userData.setUserData("");
		    	    	fileSystem[firstFreeBlock].userData.setBack(0);
		    	    	fileSystem[firstFreeBlock].userData.setForward(0);
		    	    	blockType = blockUsage.USERDATA;
		    	    	openedFileBlockNumber = firstFreeBlock;
		    	    	break;
		    	    case "D":
		    	    	fileSystem[firstFreeBlock].setBlockUsageType(blockUsage.DIRECTORY);
		    	    	blockType = blockUsage.DIRECTORY;
		    	        break;
		    	}
		    	if (fileSystem[blockNumber].getDirectoryStructure().checkIfSpaceExistsinBlock())
		    	{
		    		fileSystem[blockNumber].directoryStructure.addElementToDirectoryContents(subnames[pos], firstFreeBlock, 0, blockType);
		    	}
		    	else
		    	{
		    		boolean isEntryAdded = false;
			    	while (fileSystem[blockNumber].getDirectoryStructure().getFrwd() != 0)
			    	{
			    		blockNumber = fileSystem[blockNumber].getDirectoryStructure().getFrwd();
			    		if (fileSystem[blockNumber].getDirectoryStructure().checkIfSpaceExistsinBlock())
			    		{
			    			fileSystem[blockNumber].directoryStructure.addElementToDirectoryContents(subnames[pos], firstFreeBlock, 0, blockType);
			    			isEntryAdded = true;
			    			break;
			    		}
			    	}
			    	if (isEntryAdded == false)
			    	{
		    		    int nextFreeBlock = getFirstFreeBlock();
			    	    if (nextFreeBlock == -1)
			        	{
			    	    	System.out.println("NO MORE FREE BLOCKS while Extending dir.. Reseting");
			    	    	printBlocksStatistics();
			    	    	resetBlock(firstFreeBlock);
			    	    	return false;
			    	    }
			    	    else
			    	    {
			    		    fileSystem[blockNumber].getDirectoryStructure().setFrwd(nextFreeBlock);
			    		    fileSystem[nextFreeBlock].setBlockUsageType(blockUsage.DIRECTORY);
			    		    fileSystem[nextFreeBlock].getDirectoryStructure().setFrwd(0);
			    		    fileSystem[nextFreeBlock].directoryStructure.addElementToDirectoryContents(subnames[pos], firstFreeBlock, 0, blockType);
			    	        blockNumber = nextFreeBlock;
			    	    }
			    	}
		    	}
			}
            else
            {
            	int tmpBlockNumber;
            	do {
            		tmpBlockNumber = fileSystem[blockNumber].getDirectoryStructure().checkifDirExistsInSpecifiedBlock(subnames[pos]);
            		if (tmpBlockNumber != -1 || fileSystem[blockNumber].getDirectoryStructure().getFrwd() == 0)
            		{
            			break;
            		}
            		else
            		{
            			blockNumber = fileSystem[blockNumber].getDirectoryStructure().getFrwd();
            		}
            	}while (true);
         
            	if (tmpBlockNumber == -1)
			    {
            		System.out.println("Directory " + subnames[pos] + " doesn't exists");
            		return false;
			    }
            	blockNumber = tmpBlockNumber;
            }
		    pos ++;
		}
		if (blockType == blockUsage.INVALID || (blockType == blockUsage.USERDATA && openedFileBlockNumber == -1))
		{
			System.out.println("Software error Please check the code: " + getLineNumber());
			return false;
		}
		else if (blockType == blockUsage.USERDATA)
		{
			openFileBlockNumber = openedFileBlockNumber;
			openFilesDirectory = blockNumber;
			openFileMode = fileOpenMode.OUTPUT;
			openFileSeekPosition = 0;
		}
		return true;
	}


	private static void resetBlock(int block) {
		fileSystem[block].blockUsageType = FileSystem.blockUsage.FREE;
		fileSystem[block].getDirectoryStructure().resetDirectoryStructure();
		fileSystem[block].getUserData().resetUserDataStructure();
	}


	public blockUsage getBlockUsageType() {
		return blockUsageType;
	}


	public void setBlockUsageType(blockUsage blockUsageType) {
		this.blockUsageType = blockUsageType;
	}

	private static int getFirstFreeBlock() {
		for (int block = 0; block < FileSystem.TOTAL_BLOCKS; block++)
		{
			if(fileSystem[block].getBlockUsageType() == blockUsage.FREE)
			{
				return block;
			}
		}
		return -1;
	}


	public static void printBlocksStatistics()
	{
		for (int block = 0; block < FileSystem.TOTAL_BLOCKS; block++)
		{
			System.out.println("<--------- Block Number " + (block) + " ------->");
			System.out.println("Type: " + fileSystem[block].getBlockUsageType());
			if (fileSystem[block].getBlockUsageType() != FileSystem.blockUsage.FREE)
			{
				if (fileSystem[block].getBlockUsageType() == blockUsage.DIRECTORY)
				{
			        System.out.println("DIRECTORY STRUCTURE");
			        fileSystem[block].getDirectoryStructure().printDirectoryStatistics();
				}
				else if (fileSystem[block].getBlockUsageType() == blockUsage.USERDATA)
				{
			        System.out.println("USERDATA STRUCTURE");
			        fileSystem[block].getUserData().printUserDataStatistics();
				}
			    System.out.println("\n\n");
			}
		}
	}
};

class DirectoryStructure{
	private int back;
	private int frwd;
	private int free;

	private char[] filler;
	private DirectoryContents[] directoryContents;
	
	DirectoryStructure(){
		filler = new char[2];
		directoryContents = new DirectoryContents[FileSystem.MAX_ENTRIES_IN_A_DIR];
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			directoryContents[loop] = new DirectoryContents();
		}
		back = 0;
		frwd = 0;
	}
	
	public int getFrwd() {
		return frwd;
	}

	public int getFree() {
		return free;
	}

	public void setFree(int free) {
		this.free = free;
	}
	
	public void UpdateLastFreeBytes(int openFileBlockNumber, int size) {
		boolean isUpdated = false;
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if (directoryContents[loop].getLink() == openFileBlockNumber)
			{
				if(directoryContents[loop].getDirectoryContentType() != FileSystem.blockUsage.USERDATA)
				{
					System.out.println("Software error.. block number not seem to be file.. check the flow. line: " + FileSystem.getLineNumber());
				    return;
				}
				directoryContents[loop].setSize(size);
				isUpdated = true;
			}
		}
		if (isUpdated == false)
		{
			System.out.println("Software error.. block number not found in the directory.. check the flow. line: " + FileSystem.getLineNumber());
		}
	}

	public boolean IsDirectoryEmpty() {
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if ((directoryContents[loop].getDirectoryContentType() != FileSystem.blockUsage.FREE) && (directoryContents[loop].getDirectoryContentType() != FileSystem.blockUsage.INVALID))
			{
				return false;
			}
		}
		return true;
	}

	public void printDirectoryStatistics() {
		System.out.println("    BACK: " + back);
		System.out.println("    FRWD: " + frwd);
		System.out.println("    First FREE block: " + free);
		System.out.println("    DIRECTORY CONTENTS");
		System.out.println("        TYPE           LINK      FBYTES   NAME");
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			directoryContents[loop].printDirectoryContentsStatistics();
		}
	}

	public void setFrwd(int frwd) {
		this.frwd = frwd;
	}

	public int checkifDirExistsInSpecifiedBlock(String dirName) {
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if (directoryContents[loop].getName().equals(dirName) && directoryContents[loop].getDirectoryContentType() == FileSystem.blockUsage.DIRECTORY)
			{
				return directoryContents[loop].getLink();
			}
		}
		return -1;
	}
	
	public int checkifDirOrFileExistsInSpecifiedBlockAndGetLink(String dirOrFileName) {
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if (directoryContents[loop].getName().equals(dirOrFileName))
			{
				return directoryContents[loop].getLink();
			}
		}
		return -1;
	}
	
	public FileSystem.blockUsage checkifDirOrFileExistsInSpecifiedBlockAndGetType(String dirOrFileName) {
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if (directoryContents[loop].getName().equals(dirOrFileName))
			{
				return directoryContents[loop].getDirectoryContentType();
			}
		}
		return FileSystem.blockUsage.INVALID;
	}
	
	public int checkifFileExistsInSpecifiedBlock(String dirName) {
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			//System.out.println("dir name: " + dirName + " debug name: " + directoryContents[loop].getName() + " type " + directoryContents[loop].getDirectoryContentType());
			if ((directoryContents[loop].getName().equals(dirName)) && (directoryContents[loop].getDirectoryContentType() == FileSystem.blockUsage.USERDATA))
			{
				return directoryContents[loop].getLink();
			}
		}
		return -1;
	}

	public void resetDirectoryStructure() {
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			directoryContents[loop].setDirectoryContentType(FileSystem.blockUsage.FREE);
		}
		back = 0;
		frwd = 0;
		free = 0;
	}

	public boolean addElementToDirectoryContents(String name, int blockNumber, int size, FileSystem.blockUsage type )
	{
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if (directoryContents[loop].getDirectoryContentType() == FileSystem.blockUsage.FREE)
			{
				directoryContents[loop].setDirectoryContentType(type);
				directoryContents[loop].setName(name);
				directoryContents[loop].setLink(blockNumber);
				directoryContents[loop].setSize(size);
				return true;
			}	
		}
		return false;
	}
	
	public void deleteAnEntryInDirectoryContents(String name)
	{
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if (directoryContents[loop].getName().equals(name))
			{
				System.out.println("Reseting the directory content with name " + name);
				directoryContents[loop].resetDirectoryContent();
				break;
			}
		}
	}
	
	public boolean checkIfSpaceExistsinBlock()
	{
		for (int loop = 0; loop < FileSystem.MAX_ENTRIES_IN_A_DIR; loop++)
		{
			if (directoryContents[loop].getDirectoryContentType() == FileSystem.blockUsage.FREE)
			{
				return true;
			}	
		}
		return false;
	}

};

class UserData{
	private int back;
	private int forward;
	private String userData;
	
	UserData()
	{
		forward = -1;
		back = -1;
		userData = "";
	}

	public void resetUserDataStructure() {
		forward = -1;
		back = -1;
		userData = "";
	}

	public void printUserDataStatistics() {
		System.out.println("    BACK   FORWARD  DATA");
		System.out.println("    " + back + "  " + forward + "  " + userData);
	}

	public int getBack() {
		return back;
	}

	public void setBack(int back) {
		this.back = back;
	}

	public int getForward() {
		return forward;
	}

	public void setForward(int forward) {
		this.forward = forward;
	}

	public String getUserData() {
		return userData;
	}

	public void setUserData(String userData) {
		this.userData = userData;
	}
};

class DirectoryContents{
	private String name;
	private int size;
	private FileSystem.blockUsage directoryContentType;
	private int link;

	public String getName() {
		return name;
	}

	public void printDirectoryContentsStatistics() {
		if (directoryContentType == FileSystem.blockUsage.DIRECTORY || directoryContentType == FileSystem.blockUsage.USERDATA)
		    System.out.println("        " + directoryContentType + "  " + link + "  " + size + "  " + name);
		else
			System.out.println("        " + directoryContentType + "           " + link + "  " + size + "  " + name);
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getLink() {
		return link;
	}

	public void setLink(int link) {
		this.link = link;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public FileSystem.blockUsage getDirectoryContentType() {
		return directoryContentType;
	}

	public void setDirectoryContentType(FileSystem.blockUsage directoryContentType) {
		this.directoryContentType = directoryContentType;
	}

	DirectoryContents()
	{
		directoryContentType = FileSystem.blockUsage.FREE;
		name = "";
		link = -1;
		size = 0;
	}
	
	public void resetDirectoryContent()
	{
		directoryContentType = FileSystem.blockUsage.FREE;
		name = "";
		link = -1;
		size = 0;
	}
};
