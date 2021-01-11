


-- create the table
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

IF OBJECT_ID('[dbo].[Transactions]','U') IS NULL
BEGIN
	CREATE TABLE [dbo].[Transactions](
	[transactionId] [nvarchar](50) NOT NULL,
	[action] [nvarchar](50) NULL,
	[blobKey] [nvarchar](max) NULL,
	[attributes] [nvarchar](max) NULL,
	[event] [nvarchar](100) NOT NULL,
	[insertDateTime] [datetime] NULL,
	[status] [nvarchar](50) NOT NULL,
	[runid] [nvarchar](50) NULL,
	[workflow] [nvarchar](50) NULL,
	[eventMessage] [nvarchar](max) NULL
	) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
END
GO


-- create the index
SET ANSI_PADDING ON
GO

/****** Object:  Index [nci_wi_Transactions]    Script Date: 11/12/2019 8:52:21 AM ******/
if(0 = (SELECT COUNT(*) as index_count FROM sys.indexes  WHERE object_id = OBJECT_ID('dbo.Transactions') AND name='nci_wi_Transactions'))
BEGIN
	CREATE NONCLUSTERED INDEX [nci_wi_Transactions] ON [dbo].[Transactions]
	(
	 [event] ASC,
	 [transactionId] ASC,
	 [insertDateTime] ASC,
	 [status] ASC
	)
	INCLUDE([action],[attributes],[blobKey],[runid],[workflow]) WITH (STATISTICS_NORECOMPUTE = OFF, DROP_EXISTING = OFF, ONLINE = OFF) ON [PRIMARY]
END
GO


if OBJECT_ID('[dbo].[TRANSACTIONS_SUMMARY]','P') IS NOT NULL
DROP PROCEDURE [dbo].[TRANSACTIONS_SUMMARY]
GO

CREATE PROCEDURE  [dbo].[TRANSACTIONS_SUMMARY] (
@Hours as int = -24) AS

BEGIN 
	
	SELECT * 
	INTO #TABLEEXTRACT
	FROM [dbo].[Transactions]
	WHERE [insertDateTime] > DATEADD(hour, @Hours, GETDATE()) 

	SELECT [transactionId], [event], MAX([insertDateTime]) [timestamp] 
	INTO #TABLEEXTRACTSUM
	FROM #TABLEEXTRACT  
	GROUP BY [transactionId],[event] 

	
SELECT event, total, ISNULL(fails, 0) as fails, ISNULL(incomplete, 0) as incomplete FROM ( 
	SELECT tot.event as event, count(*) as total FROM ( 
		SELECT t1.transactionId, t1.event, t1.action, t1.status,t1.insertDateTime,t1.attributes,t1.blobkey FROM #TABLEEXTRACT t1 
		JOIN (SELECT * FROM #TABLEEXTRACTSUM) t2 
		ON [t1].[transactionId] = [t2].[transactionId] 
		AND [t1].[event] = [t2].[event] 
		AND [t1].[insertDateTime] = [t2].[timestamp] 
	) tot  
	GROUP BY tot.event  
) a 
LEFT JOIN (  
	SELECT fal.event as failedevent, count(*) as fails FROM ( 
			SELECT t3.transactionId, t3.event, t3.action, t3.status,t3.insertDateTime,t3.attributes,t3.blobkey FROM #TABLEEXTRACT t3  
			JOIN (SELECT * FROM #TABLEEXTRACTSUM) t4  
			ON [t3].[transactionId] = [t4].[transactionId]  
			AND [t3].[event] = [t4].[event]  
			AND [t3].[insertDateTime] = [t4].[timestamp]  
			WHERE status = 'Failed'  
	) fal  
	GROUP BY fal.event  
) f  
ON a.event = f.failedevent  
LEFT JOIN (  
	SELECT fal.event as incompleteevent, count(*) as incomplete FROM ( 
			SELECT t3.transactionId, t3.event, t3.action, t3.status,t3.insertDateTime,t3.attributes,t3.blobkey FROM #TABLEEXTRACT t3  
			JOIN (  
				SELECT [transactionId], MAX([timestamp]) [timestamp]   
				FROM  #TABLEEXTRACTSUM  
				GROUP BY [transactionId]  
			) t4  
			ON [t3].[transactionId] = [t4].[transactionId]  
			AND [t3].[insertDateTime] = [t4].[timestamp]  
			WHERE status = 'Succeeded'  
			AND NOT action like 'Routing%' 
	) fal  
	GROUP BY fal.event  
) u  
ON a.event = u.incompleteevent 
order by event 

DROP TABLE IF EXISTS #TABLEEXTRACT
DROP TABLE IF EXISTS #TABLEEXTRACTSUM

END
GO

if OBJECT_ID('[dbo].[TRANSACTIONS_CLEANUP]','P') IS NOT NULL
DROP PROCEDURE [dbo].[TRANSACTIONS_CLEANUP]
GO


CREATE PROCEDURE [dbo].[TRANSACTIONS_CLEANUP]
(
	@past_days AS INT = -14
)
AS
BEGIN
	-- SET NOCOUNT ON added to prevent extra result sets from
	-- interfering with SELECT statements.
	SET NOCOUNT ON

	IF (@past_days <= -14) -- Prevent from accidental deleting of records less than 14 days.
		BEGIN
			delete from [dbo].[Transactions] 
			WHERE [insertDateTime] <   DATEADD(day, @past_days, GETDATE());
		END 

END

GO

