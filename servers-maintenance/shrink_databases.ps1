$SQLInstance = $env:computername

Invoke-SQLCmd -serverinstance $SQLInstance -querytimeout 0 -query "
CREATE TABLE #DatabaseShrinkList (ID INT IDENTITY, NAME NVARCHAR(100))

INSERT #DatabaseShrinkList
SELECT NAME FROM sys.databases WHERE IS_READ_ONLY=0 and STATE=0 and NAME NOT IN ('master','model','msdb','tempdb') and NAME NOT LIKE ('%template%')
DECLARE @counter INT = 1
DECLARE @NumberOfDBs INT = 0

SELECT @NumberOfDBs = COUNT(0) FROM #DatabaseShrinkList
DECLARE @DBName NVARCHAR(100), @SQL NVARCHAR(MAX)
WHILE (@counter < @NumberOfDBs)

BEGIN
    SELECT @DBName = Name FROM #DatabaseShrinkList WHERE ID = @counter
    SELECT @SQL = 'ALTER DATABASE [' + @DBName + '] SET RECOVERY SIMPLE'
    PRINT(@SQL)
    EXEC(@SQL)
    DBCC SHRINKDATABASE (@DBName , 0)   
    SET @counter = @counter + 1
END

DROP TABLE #DatabaseShrinkList"
