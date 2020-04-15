#Global variables
#============================================================================
my $API_URL = 'https://s.bit-erp.ru/rest/scriptrunner/latest/custom/zabbix';
my $PROJECT_KEY;
my $PROJECT_TYPE;
my $SUMMARY;
my $DESCRIPTION;


#============================================================================
create_task();

#============================================================================
sub create_task
{
    system('clear');

    GetOptions ('s=s' => \$SUMMARY,
    'd=s' => \$DESCRIPTION,
    'k=s' => \$PROJECT_KEY,
    't=s' => \$PROJECT_TYPE,
);


    my $data = fill_json();

    my $client = REST::Client->new();

    $client->POST($API_URL, $data);

    my $responseCode = $client->responseCode();

    if ($responseCode != 200)
    {
        print "Error, response status: $client->responseCode()\n";


        exit 0;
    }

    print "Ticket is created\n";
}


sub fill_json
{
    my %json;

    $json{'fields'}{'project'}{'key'} = $PROJECT_KEY;
    $json{'fields'}{'summary'} = $SUMMARY;
    $json{'fields'}{'description'} = $DESCRIPTION;
    $json{'fields'}{'issuetype'}{'name'} = $PROJECT_TYPE;
    $json{'fields'}{'priority'}{'id'} = '2';

    #Priority id
    #1 - Blocker
    #2 - Critical
    #3 - General
    #4 - Minor
    #5 - Trivial

    return encode_json(\%json);
}
