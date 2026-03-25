import Menubar from "../components/Menubar.jsx";
import Header from "../components/Header.jsx";

const Home = () => {
    return (
        <div className="flex flex-column align-items-center justify-content-center min-vh-100">
            <Menubar />
            <Header />
        </div>

    )
}
export default Home;