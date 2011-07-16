use HTTP::Request::Common;
use LWP::UserAgent;
use JSON;

$filename = 'booking';
$deploymentname = $filename . '.play';

$ua = LWP::UserAgent->new;

$json = JSON->new->allow_nonref;

$deploy = $json->encode({"operation" => "add",
			     	    "address" => [{"deployment" => $deploymentname}],
                                    "enabled" => "true",
                                    "content" => [{"archive" => true, "path" => $filename, "relative-to" => "play.framework"}]});

$resp = $ua->request(POST 'http://localhost:9990/management', Content=>$deploy);

die $resp->status_line if ! $resp->is_success;
print "Success!!!\n"
